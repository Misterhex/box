# Kafka mTLS Platform - Full Deployment Plan

## Overview

Deploy a production-grade Kafka mTLS platform on DigitalOcean with:
- 3 Kafka controllers + 3 brokers (KRaft mode, mTLS)
- DOKS Kubernetes cluster with Schema Registry, Kafka UI, Prometheus
- Synthetic producer/consumer for end-to-end validation
- Full teardown after validation

## Architecture

```
DigitalOcean VPC (10.10.0.0/16)
├── kafka-controller-{1,2,3}   (s-1vcpu-2gb, KRaft controllers, port 9094 SSL)
├── kafka-broker-{1,2,3}       (s-2vcpu-4gb, KRaft brokers, ports 9092/9093 SSL)
└── DOKS Kubernetes Cluster     (2x s-2vcpu-4gb nodes)
    ├── NGINX Ingress (LoadBalancer)
    ├── Schema Registry (2 replicas, mTLS to Kafka)
    ├── Kafka UI (1 replica, mTLS to Kafka)
    ├── Prometheus (JMX scraping)
    └── Synthetic Client (producer + consumer, mTLS, Avro)
```

## Credentials

- **DO Token**: Set as `export TF_VAR_do_token="<token>"`
- **SSH Key**: Generate fresh, upload to DO, use fingerprint
- **Kafka passwords**: `changeit` (keystore/truststore)

## Prerequisites (install if missing)

```bash
# Terraform >= 1.5
# Ansible >= 2.15
# kubectl
# helm >= 3.12
# Java 17+ (for cert-tool, only needed if building locally)
# keytool (comes with JDK)
# openssl
# doctl (optional, for SSH key management)
# jq
```

---

## PHASE 1: SSH Key Generation & Upload

```bash
# Generate SSH key pair (if not already exists)
ssh-keygen -t ed25519 -f ~/.ssh/kafka_deploy -N "" -C "kafka-deploy"

# Upload to DigitalOcean via API
SSH_PUB_KEY=$(cat ~/.ssh/kafka_deploy.pub)
FINGERPRINT=$(curl -s -X POST "https://api.digitalocean.com/v2/account/ssh_keys" \
  -H "Authorization: Bearer ${TF_VAR_do_token}" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"kafka-deploy\", \"public_key\": \"$SSH_PUB_KEY\"}" \
  | jq -r '.ssh_key.fingerprint')
echo "SSH Fingerprint: $FINGERPRINT"

# Export for Terraform
export TF_VAR_ssh_key_fingerprints="[\"$FINGERPRINT\"]"
```

---

## PHASE 2: Terraform Infrastructure Provisioning

```bash
cd terraform/

# Create tfvars
cat > terraform.tfvars <<EOF
do_token             = "${TF_VAR_do_token}"
ssh_key_fingerprints = ${TF_VAR_ssh_key_fingerprints}
region               = "sgp1"
controller_count     = 3
broker_count         = 3
controller_size      = "s-1vcpu-2gb"
broker_size          = "s-2vcpu-4gb"
k8s_node_size        = "s-2vcpu-4gb"
k8s_node_count       = 2
EOF

terraform init
terraform apply -auto-approve

# Capture outputs
terraform output -json > /tmp/tf-outputs.json
```

**Outputs generated:**
- `terraform/generated/inventory.yml` - Ansible inventory with real IPs
- `terraform/generated/kubeconfig.yaml` - K8s cluster access

**Wait for cloud-init** to finish on all droplets (~2-3 min):
```bash
for ip in $(terraform output -json broker_public_ips | jq -r '.[]') \
          $(terraform output -json controller_public_ips | jq -r '.[]'); do
  echo "Waiting for $ip..."
  until ssh -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no root@$ip "cloud-init status --wait" 2>/dev/null; do
    sleep 5
  done
done
```

---

## PHASE 3: TLS Certificate Generation

Generate CA, broker wildcard cert, and client certs. Use `openssl` + `keytool` directly (faster than building the Java cert-tool).

```bash
mkdir -p /tmp/kafka-certs && cd /tmp/kafka-certs

# 3a. Generate CA
openssl req -new -x509 -keyout ca-key.pem -out ca-cert.pem -days 365 \
  -subj "/CN=Kafka-CA/O=KafkaPlatform" -passout pass:changeit -nodes

# 3b. Create truststore (shared by all nodes)
keytool -import -keystore kafka.truststore.jks -alias CARoot \
  -file ca-cert.pem -storepass changeit -noprompt

# 3c. For each node, create keystore with signed cert
# NOTE: CN must match what Kafka uses for authentication:
#   - Brokers: CN=kafka-broker (matches super.users=User:kafka-broker)
#   - Controllers: CN=kafka-controller
#   - Schema Registry client: CN=schema-registry
#   - Kafka UI client: CN=kafka-ui
#   - Synthetic client: CN=synthetic-client

generate_cert() {
  local NAME=$1
  local CN=$2
  local SAN=$3  # optional SAN entries

  # Generate key pair + keystore
  keytool -genkey -keystore ${NAME}.keystore.jks -alias kafka \
    -validity 365 -keyalg RSA -keysize 2048 \
    -storepass changeit -keypass changeit \
    -dname "CN=${CN},O=KafkaPlatform"

  # Create CSR
  keytool -certreq -keystore ${NAME}.keystore.jks -alias kafka \
    -file ${NAME}.csr -storepass changeit

  # Sign with CA (with SANs if provided)
  if [ -n "$SAN" ]; then
    openssl x509 -req -CA ca-cert.pem -CAkey ca-key.pem -in ${NAME}.csr \
      -out ${NAME}-signed.pem -days 365 -CAcreateserial \
      -extfile <(echo "subjectAltName=${SAN}")
  else
    openssl x509 -req -CA ca-cert.pem -CAkey ca-key.pem -in ${NAME}.csr \
      -out ${NAME}-signed.pem -days 365 -CAcreateserial
  fi

  # Import CA cert into keystore
  keytool -import -keystore ${NAME}.keystore.jks -alias CARoot \
    -file ca-cert.pem -storepass changeit -noprompt

  # Import signed cert into keystore
  keytool -import -keystore ${NAME}.keystore.jks -alias kafka \
    -file ${NAME}-signed.pem -storepass changeit -noprompt
}

# Get IPs from Terraform output
cd /path/to/repo/terraform
BROKER_PRIVATE_IPS=$(terraform output -json broker_private_ips | jq -r 'to_entries[].value')
CONTROLLER_PRIVATE_IPS=$(terraform output -json controller_private_ips | jq -r 'to_entries[].value')
BROKER_PUBLIC_IPS=$(terraform output -json broker_public_ips | jq -r 'to_entries[].value')
CONTROLLER_PUBLIC_IPS=$(terraform output -json controller_public_ips | jq -r 'to_entries[].value')
cd /tmp/kafka-certs

# Generate broker certs (all brokers share CN=kafka-broker for super.users match)
i=1
for ip in $BROKER_PRIVATE_IPS; do
  pub_ip=$(echo $BROKER_PUBLIC_IPS | awk "{print \$$i}")
  SAN="DNS:kafka-broker-${i},IP:${ip},IP:${pub_ip}"
  generate_cert "broker-${i}" "kafka-broker" "$SAN"
  i=$((i+1))
done

# Generate controller certs
i=1
for ip in $CONTROLLER_PRIVATE_IPS; do
  pub_ip=$(echo $CONTROLLER_PUBLIC_IPS | awk "{print \$$i}")
  SAN="DNS:kafka-controller-${i},IP:${ip},IP:${pub_ip}"
  generate_cert "controller-${i}" "kafka-controller" "$SAN"
  i=$((i+1))
done

# Generate client certs for K8s services
generate_cert "schema-registry-client" "schema-registry" ""
generate_cert "kafka-ui-client" "kafka-ui" ""
generate_cert "synthetic-client" "synthetic-client" ""
```

---

## PHASE 4: Distribute Certificates to Nodes

```bash
cd /tmp/kafka-certs

# Distribute to brokers
i=1
for ip in $BROKER_PUBLIC_IPS; do
  ssh -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no root@$ip "mkdir -p /etc/kafka/ssl"
  scp -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no \
    broker-${i}.keystore.jks root@$ip:/etc/kafka/ssl/kafka.keystore.jks
  scp -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no \
    kafka.truststore.jks root@$ip:/etc/kafka/ssl/kafka.truststore.jks
  ssh -i ~/.ssh/kafka_deploy root@$ip "chown -R kafka:kafka /etc/kafka/ssl && chmod 600 /etc/kafka/ssl/*.jks"
  i=$((i+1))
done

# Distribute to controllers
i=1
for ip in $CONTROLLER_PUBLIC_IPS; do
  ssh -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no root@$ip "mkdir -p /etc/kafka/ssl"
  scp -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no \
    controller-${i}.keystore.jks root@$ip:/etc/kafka/ssl/kafka.keystore.jks
  scp -i ~/.ssh/kafka_deploy -o StrictHostKeyChecking=no \
    kafka.truststore.jks root@$ip:/etc/kafka/ssl/kafka.truststore.jks
  ssh -i ~/.ssh/kafka_deploy root@$ip "chown -R kafka:kafka /etc/kafka/ssl && chmod 600 /etc/kafka/ssl/*.jks"
  i=$((i+1))
done
```

---

## PHASE 5: Fix Ansible Configuration for Real IPs

### Critical issues to fix before running Ansible:

**Issue 1: Hostname resolution**
The broker `server.properties.j2` uses `{{ ansible_hostname }}` in `advertised.listeners`, but other nodes can't resolve these hostnames. Fix: use the `private_ip` variable from the generated inventory instead.

Edit `ansible/roles/kafka-broker/templates/server.properties.j2`:
```properties
# Change:
advertised.listeners=BROKER://{{ ansible_hostname }}:9092,CLIENT://{{ ansible_hostname }}:9093
# To:
advertised.listeners=BROKER://{{ private_ip }}:9092,CLIENT://{{ private_ip }}:9093
```

**Issue 2: Controller quorum voters**
The `group_vars/kafka_brokers.yml` and `kafka_controllers.yml` hardcode `kafka_controller_quorum_voters` with hostname references (`controller-1`, etc.). The Terraform-generated inventory provides per-host `kafka_controller_quorum` with actual IPs.

Edit `ansible/roles/kafka-broker/templates/server.properties.j2` and `ansible/roles/kafka-controller/templates/server.properties.j2`:
```properties
# Change:
controller.quorum.voters={{ kafka_controller_quorum_voters }}
# To:
controller.quorum.voters={{ kafka_controller_quorum }}
```

**Issue 3: Java package version**
The `group_vars/all.yml` installs `openjdk-17-jdk` but the Terraform cloud-init already installs `openjdk-21-jre-headless`. Either is fine for Kafka 3.8.1. Ensure consistency by changing `all.yml`:
```yaml
java_packages:
  - openjdk-21-jre-headless
```

**Issue 4: SSH key path**
The generated inventory uses `ansible_user: root`. Ensure Ansible uses the correct SSH key:
```bash
export ANSIBLE_PRIVATE_KEY_FILE=~/.ssh/kafka_deploy
```

### Run Ansible

```bash
cd ansible/

# Copy Terraform-generated inventory
cp ../terraform/generated/inventory.yml inventory/hosts.yml

# Set SSH key
export ANSIBLE_PRIVATE_KEY_FILE=~/.ssh/kafka_deploy

# Deploy (controllers first, then brokers)
ansible-playbook -i inventory/hosts.yml playbooks/site.yml -v

# Verify Kafka is running on all nodes
ansible all -i inventory/hosts.yml -m shell -a "systemctl status kafka --no-pager" -b
```

---

## PHASE 6: Create K8s Secrets for mTLS & Fix Helm Charts

The Terraform `helm.tf` already deploys Schema Registry, Kafka UI, and Prometheus. But those services need mTLS credentials to talk to the Kafka brokers.

### Option A: Enable mTLS on K8s services (production-correct)

```bash
export KUBECONFIG=$(pwd)/../terraform/generated/kubeconfig.yaml

# Create namespace if not exists
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -

# Create K8s secrets with client certs
cd /tmp/kafka-certs

kubectl -n kafka create secret generic schema-registry-kafka-tls \
  --from-file=keystore.jks=schema-registry-client.keystore.jks \
  --from-file=truststore.jks=kafka.truststore.jks \
  --from-literal=keystore-password=changeit \
  --from-literal=truststore-password=changeit

kubectl -n kafka create secret generic kafka-ui-tls \
  --from-file=keystore.jks=kafka-ui-client.keystore.jks \
  --from-file=truststore.jks=kafka.truststore.jks \
  --from-literal=keystore-password=changeit \
  --from-literal=truststore-password=changeit
```

Then update the Helm values in `terraform/helm.tf` to enable TLS:
- Schema Registry: set `kafka.tls.enabled=true` and `kafka.tls.existingSecret=schema-registry-kafka-tls`
- Kafka UI: set `tls.enabled=true` and `tls.existingSecret=kafka-ui-tls`

### Option B: Add PLAINTEXT listener to brokers (simpler for demo)

Add a PLAINTEXT listener on the brokers for VPC-internal traffic from K8s:

Edit `ansible/roles/kafka-broker/templates/server.properties.j2`:
```properties
listeners=BROKER://:9092,CLIENT://:9093,INTERNAL://:9094
advertised.listeners=BROKER://{{ private_ip }}:9092,CLIENT://{{ private_ip }}:9093,INTERNAL://{{ private_ip }}:9094
listener.security.protocol.map=BROKER:SSL,CLIENT:SSL,INTERNAL:PLAINTEXT,CONTROLLER:SSL
```

> **Recommendation**: Use Option A (mTLS everywhere) for a proper mTLS demo.

### ACLs for client access

With `allow.everyone.if.no.acl.found=false`, clients need ACLs. For initial testing, temporarily set to `true`:

Edit `ansible/roles/kafka-broker/templates/server.properties.j2`:
```properties
allow.everyone.if.no.acl.found=true
```

Or create proper ACLs after Kafka is running (from a broker node):
```bash
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9093 \
  --command-config /tmp/admin.properties \
  --add --allow-principal User:schema-registry \
  --operation All --topic _schemas --group '*'

/opt/kafka/kafka_2.13-3.8.1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9093 \
  --command-config /tmp/admin.properties \
  --add --allow-principal User:synthetic-client \
  --operation All --topic synthetic-events --group '*'
```

### Re-run Terraform to update Helm releases

```bash
cd terraform/
terraform apply -auto-approve
```

---

## PHASE 7: Deploy Synthetic Producer/Consumer

The synthetic client is a Spring Boot app. Deploy it as a K8s Job:

### Build and containerize (requires Docker)

```bash
cd kafka-synthetic-client/
./gradlew build -x test

# Create Dockerfile if not exists
cat > Dockerfile <<'EOF'
FROM eclipse-temurin:21-jre
COPY build/libs/kafka-synthetic-client-0.1.0.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EOF

docker build -t kafka-synthetic-client:latest .
# Push to a registry accessible by DOKS, or use DO Container Registry
```

### Alternative: Run from a broker node directly

```bash
# Copy the JAR and certs to a broker
BROKER_IP=$(terraform -chdir=terraform output -json broker_public_ips | jq -r 'to_entries[0].value')
scp -i ~/.ssh/kafka_deploy kafka-synthetic-client/build/libs/*.jar root@$BROKER_IP:/tmp/
scp -i ~/.ssh/kafka_deploy /tmp/kafka-certs/synthetic-client.keystore.jks root@$BROKER_IP:/tmp/
scp -i ~/.ssh/kafka_deploy /tmp/kafka-certs/kafka.truststore.jks root@$BROKER_IP:/tmp/

# Get Schema Registry URL (from K8s ingress LB IP)
LB_IP=$(kubectl --kubeconfig=terraform/generated/kubeconfig.yaml -n ingress-nginx \
  get svc ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Run synthetic client on broker (3 minutes)
ssh -i ~/.ssh/kafka_deploy root@$BROKER_IP "
  timeout 180 java -jar /tmp/kafka-synthetic-client-0.1.0.jar \
    --kafka.bootstrap-servers=localhost:9093 \
    --kafka.ssl.keystore-location=/tmp/synthetic-client.keystore.jks \
    --kafka.ssl.keystore-password=changeit \
    --kafka.ssl.truststore-location=/tmp/kafka.truststore.jks \
    --kafka.ssl.truststore-password=changeit \
    --kafka.schema-registry.url=http://${LB_IP}:8081 \
    --kafka.producer.rate-per-second=10 \
    --kafka.topic=synthetic-events \
    2>&1 | tee /tmp/synthetic-report.txt
"

# Retrieve report
scp -i ~/.ssh/kafka_deploy root@$BROKER_IP:/tmp/synthetic-report.txt /tmp/synthetic-report.txt
cat /tmp/synthetic-report.txt
```

---

## PHASE 8: Validation

### Kafka cluster health
```bash
# From a broker node
ssh -i ~/.ssh/kafka_deploy root@$BROKER_IP "
  /opt/kafka/kafka_2.13-3.8.1/bin/kafka-metadata.sh \
    --snapshot /var/lib/kafka/broker-logs/__cluster_metadata-0/00000000000000000000.log \
    --cluster-id MkU3OEVBNTcwNTJENDM2Qk 2>&1 | head -20
"
```

### Kafka UI validation
```bash
LB_IP=$(kubectl --kubeconfig=terraform/generated/kubeconfig.yaml -n ingress-nginx \
  get svc ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Check Kafka UI (use Host header since no domain configured)
curl -s -H "Host: kafka-ui" http://$LB_IP/api/clusters
```

### Prometheus metrics
```bash
curl -s -H "Host: prometheus" http://$LB_IP/api/v1/targets | jq '.data.activeTargets | length'
```

### Schema Registry
```bash
curl -s -H "Host: schema-registry" http://$LB_IP/subjects
```

### K8s pod health
```bash
kubectl --kubeconfig=terraform/generated/kubeconfig.yaml -n kafka get pods
kubectl --kubeconfig=terraform/generated/kubeconfig.yaml -n ingress-nginx get pods
```

---

## PHASE 9: Teardown

```bash
cd terraform/
terraform destroy -auto-approve

# Clean up SSH key from DO (optional)
curl -s -X DELETE "https://api.digitalocean.com/v2/account/ssh_keys/${FINGERPRINT}" \
  -H "Authorization: Bearer ${TF_VAR_do_token}"

# Clean up local certs
rm -rf /tmp/kafka-certs
```

---

## Known Issues & Fixes

### 1. `advertised.listeners` uses hostname instead of IP
**File**: `ansible/roles/kafka-broker/templates/server.properties.j2`
**Fix**: Replace `{{ ansible_hostname }}` with `{{ private_ip }}`

### 2. Controller quorum voters hardcoded with hostnames
**Files**: `ansible/group_vars/kafka_brokers.yml`, `ansible/group_vars/kafka_controllers.yml`
**Fix**: Use `{{ kafka_controller_quorum }}` from Terraform-generated inventory instead of the hardcoded `kafka_controller_quorum_voters`

### 3. Schema Registry / Kafka UI can't connect without mTLS certs
**Fix**: Create K8s secrets with client keystores/truststores, enable TLS in Helm values

### 4. ACLs block all non-super-user access
**Fix**: Set `allow.everyone.if.no.acl.found=true` for testing, or create explicit ACLs

### 5. Java version mismatch
**File**: `ansible/group_vars/all.yml`
**Fix**: Change `openjdk-17-jdk` to `openjdk-21-jre-headless` (matches Terraform cloud-init)

### 6. Ingress routing without domain
When `var.domain` is empty, ingress hosts are empty strings. You must use Host headers or configure a domain. For LB IP-only access, consider using path-based routing or NodePort services instead.

---

## Execution Notes for Claude Code

When running this plan in an unrestricted environment:

1. **Set environment variables first**: `TF_VAR_do_token`, `ANSIBLE_PRIVATE_KEY_FILE`
2. **Apply the code fixes** listed in "Known Issues" before running Ansible
3. **Wait for cloud-init** (~2 min) after Terraform before running Ansible
4. **Wait for controller quorum** (~30 sec) after deploying controllers before brokers
5. **The synthetic client** should run for 3 minutes and produce a report showing messages produced/consumed
6. **Always teardown** with `terraform destroy` to avoid ongoing charges
7. **Total estimated cost**: ~$0.50 for the ~20 minute session
8. **Total estimated time**: 15-20 minutes end-to-end
