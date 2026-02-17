# SSL Certificate Setup for Kafka mTLS

This guide shows how to generate SSL certificates for Kafka mTLS authentication.

## Overview

Each Kafka node (controller or broker) needs:
- **Keystore** (`kafka.keystore.jks`): Private key and signed certificate
- **Truststore** (`kafka.truststore.jks`): CA certificate to validate peers

## Option 1: Using OpenSSL and Keytool (Self-Signed CA)

### Step 1: Create Certificate Authority (CA)

```bash
# Generate CA private key and certificate
openssl req -new -x509 \
  -keyout ca-key \
  -out ca-cert \
  -days 365 \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=Kafka-CA" \
  -passout pass:changeit
```

### Step 2: Generate Certificates for Each Node

Run this for each Kafka node (controller-1, controller-2, controller-3, broker-1, broker-2, broker-3):

```bash
#!/bin/bash
NODE_NAME="controller-1"  # Change for each node
KEYSTORE_PASS="changeit"
TRUSTSTORE_PASS="changeit"
VALIDITY_DAYS=365

# 1. Generate keystore with private key
keytool -genkey -keystore ${NODE_NAME}.keystore.jks \
  -alias kafka \
  -validity ${VALIDITY_DAYS} \
  -keyalg RSA \
  -keysize 2048 \
  -storepass ${KEYSTORE_PASS} \
  -keypass ${KEYSTORE_PASS} \
  -dname "CN=${NODE_NAME},OU=Engineering,O=Organization,L=City,ST=State,C=US" \
  -ext SAN=dns:${NODE_NAME},dns:localhost,ip:127.0.0.1

# 2. Create certificate signing request (CSR)
keytool -certreq -keystore ${NODE_NAME}.keystore.jks \
  -alias kafka \
  -file ${NODE_NAME}.csr \
  -storepass ${KEYSTORE_PASS} \
  -keypass ${KEYSTORE_PASS}

# 3. Sign the certificate with CA
openssl x509 -req \
  -CA ca-cert \
  -CAkey ca-key \
  -in ${NODE_NAME}.csr \
  -out ${NODE_NAME}-cert-signed \
  -days ${VALIDITY_DAYS} \
  -CAcreateserial \
  -passin pass:changeit

# 4. Import CA certificate into keystore
keytool -import -keystore ${NODE_NAME}.keystore.jks \
  -alias CARoot \
  -file ca-cert \
  -storepass ${KEYSTORE_PASS} \
  -noprompt

# 5. Import signed certificate into keystore
keytool -import -keystore ${NODE_NAME}.keystore.jks \
  -alias kafka \
  -file ${NODE_NAME}-cert-signed \
  -storepass ${KEYSTORE_PASS} \
  -noprompt

# 6. Create truststore with CA certificate
keytool -import -keystore ${NODE_NAME}.truststore.jks \
  -alias CARoot \
  -file ca-cert \
  -storepass ${TRUSTSTORE_PASS} \
  -noprompt

echo "Generated certificates for ${NODE_NAME}:"
echo "  - ${NODE_NAME}.keystore.jks"
echo "  - ${NODE_NAME}.truststore.jks"
```

### Step 3: Distribute Certificates to Nodes

```bash
# For each node, copy the keystore and truststore
NODE_NAME="controller-1"
NODE_IP="10.0.1.1"

scp ${NODE_NAME}.keystore.jks root@${NODE_IP}:/etc/kafka/ssl/kafka.keystore.jks
scp ${NODE_NAME}.truststore.jks root@${NODE_IP}:/etc/kafka/ssl/kafka.truststore.jks

# Set ownership and permissions
ssh root@${NODE_IP} "chown kafka:kafka /etc/kafka/ssl/*.jks && chmod 600 /etc/kafka/ssl/*.jks"
```

## Option 2: Using OpenSSL Only (PEM Format, then Convert)

### Step 1: Create CA

```bash
# Generate CA private key
openssl genrsa -out ca-key.pem 2048

# Generate CA certificate
openssl req -new -x509 -key ca-key.pem -out ca-cert.pem -days 365 \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=Kafka-CA"
```

### Step 2: Generate Node Certificates

```bash
#!/bin/bash
NODE_NAME="controller-1"

# Generate private key
openssl genrsa -out ${NODE_NAME}-key.pem 2048

# Generate certificate signing request
openssl req -new -key ${NODE_NAME}-key.pem -out ${NODE_NAME}.csr \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=${NODE_NAME}"

# Sign with CA
openssl x509 -req -in ${NODE_NAME}.csr \
  -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out ${NODE_NAME}-cert.pem -days 365

# Convert to PKCS12 format
openssl pkcs12 -export \
  -in ${NODE_NAME}-cert.pem \
  -inkey ${NODE_NAME}-key.pem \
  -out ${NODE_NAME}.p12 \
  -name kafka \
  -CAfile ca-cert.pem \
  -caname CARoot \
  -passout pass:changeit

# Convert PKCS12 to JKS keystore
keytool -importkeystore \
  -srckeystore ${NODE_NAME}.p12 \
  -srcstoretype PKCS12 \
  -srcstorepass changeit \
  -destkeystore ${NODE_NAME}.keystore.jks \
  -deststoretype JKS \
  -deststorepass changeit \
  -noprompt

# Create truststore with CA
keytool -import -keystore ${NODE_NAME}.truststore.jks \
  -alias CARoot \
  -file ca-cert.pem \
  -storepass changeit \
  -noprompt
```

## Option 3: Automated Script for All Nodes

```bash
#!/bin/bash
# generate-all-certs.sh

set -e

KEYSTORE_PASS="changeit"
TRUSTSTORE_PASS="changeit"
VALIDITY_DAYS=365

# Node list
declare -a CONTROLLERS=("controller-1" "controller-2" "controller-3")
declare -a BROKERS=("broker-1" "broker-2" "broker-3")
ALL_NODES=("${CONTROLLERS[@]}" "${BROKERS[@]}")

# Create CA if it doesn't exist
if [ ! -f ca-cert ]; then
    echo "Creating CA..."
    openssl req -new -x509 \
      -keyout ca-key \
      -out ca-cert \
      -days ${VALIDITY_DAYS} \
      -subj "/C=US/ST=State/L=City/O=MyOrg/CN=Kafka-CA" \
      -passout pass:${KEYSTORE_PASS}
fi

# Generate certificates for each node
for NODE in "${ALL_NODES[@]}"; do
    echo "Generating certificates for ${NODE}..."

    # Generate keystore
    keytool -genkey -keystore ${NODE}.keystore.jks \
      -alias kafka \
      -validity ${VALIDITY_DAYS} \
      -keyalg RSA \
      -keysize 2048 \
      -storepass ${KEYSTORE_PASS} \
      -keypass ${KEYSTORE_PASS} \
      -dname "CN=${NODE},OU=Engineering,O=MyOrg,L=City,ST=State,C=US" \
      -ext SAN=dns:${NODE},dns:localhost

    # Create CSR
    keytool -certreq -keystore ${NODE}.keystore.jks \
      -alias kafka \
      -file ${NODE}.csr \
      -storepass ${KEYSTORE_PASS}

    # Sign with CA
    openssl x509 -req \
      -CA ca-cert \
      -CAkey ca-key \
      -in ${NODE}.csr \
      -out ${NODE}-cert-signed \
      -days ${VALIDITY_DAYS} \
      -CAcreateserial \
      -passin pass:${KEYSTORE_PASS}

    # Import CA into keystore
    keytool -import -keystore ${NODE}.keystore.jks \
      -alias CARoot \
      -file ca-cert \
      -storepass ${KEYSTORE_PASS} \
      -noprompt

    # Import signed cert into keystore
    keytool -import -keystore ${NODE}.keystore.jks \
      -alias kafka \
      -file ${NODE}-cert-signed \
      -storepass ${KEYSTORE_PASS} \
      -noprompt

    # Create truststore
    keytool -import -keystore ${NODE}.truststore.jks \
      -alias CARoot \
      -file ca-cert \
      -storepass ${TRUSTSTORE_PASS} \
      -noprompt

    echo "✓ Generated ${NODE}.keystore.jks and ${NODE}.truststore.jks"
done

echo ""
echo "All certificates generated successfully!"
echo ""
echo "Next steps:"
echo "1. Copy keystores and truststores to respective nodes:"
for NODE in "${ALL_NODES[@]}"; do
    echo "   scp ${NODE}.{keystore,truststore}.jks root@${NODE}:/etc/kafka/ssl/"
done
echo ""
echo "2. Set ownership and permissions on each node:"
echo "   chown kafka:kafka /etc/kafka/ssl/*.jks"
echo "   chmod 600 /etc/kafka/ssl/*.jks"
```

## Verification

### Check Keystore Contents

```bash
keytool -list -v -keystore kafka.keystore.jks -storepass changeit
```

Expected output:
- CA certificate (alias: CARoot)
- Node certificate (alias: kafka)

### Check Truststore Contents

```bash
keytool -list -v -keystore kafka.truststore.jks -storepass changeit
```

Expected output:
- CA certificate (alias: CARoot)

### Verify Certificate Chain

```bash
keytool -list -v -keystore kafka.keystore.jks -storepass changeit | grep -A 5 "Certificate chain"
```

Should show a valid certificate chain from the node cert to the CA.

## Client Certificates

Kafka clients also need certificates for mTLS. Generate client certificates the same way:

```bash
NODE_NAME="kafka-client-1"
# ... follow same steps as above
```

Then use in client configuration:

```properties
security.protocol=SSL
ssl.truststore.location=/path/to/kafka-client-1.truststore.jks
ssl.truststore.password=changeit
ssl.keystore.location=/path/to/kafka-client-1.keystore.jks
ssl.keystore.password=changeit
ssl.key.password=changeit
```

## Using Ansible to Distribute Certificates

You can extend the Ansible playbook to distribute certificates:

```yaml
# Add to roles/kafka-common/tasks/main.yml

- name: Copy SSL keystore
  ansible.builtin.copy:
    src: "certs/{{ inventory_hostname }}.keystore.jks"
    dest: "{{ kafka_ssl_dir }}/kafka.keystore.jks"
    owner: "{{ kafka_user }}"
    group: "{{ kafka_group }}"
    mode: '0600'
  become: yes

- name: Copy SSL truststore
  ansible.builtin.copy:
    src: "certs/{{ inventory_hostname }}.truststore.jks"
    dest: "{{ kafka_ssl_dir }}/kafka.truststore.jks"
    owner: "{{ kafka_user }}"
    group: "{{ kafka_group }}"
    mode: '0600'
  become: yes
```

Then place certificates in `ansible/files/certs/` directory:
```
ansible/files/certs/
  controller-1.keystore.jks
  controller-1.truststore.jks
  controller-2.keystore.jks
  controller-2.truststore.jks
  ...
```

## Production Considerations

1. **Use a proper PKI**: In production, use your organization's PKI/CA infrastructure
2. **Longer validity**: Use 365+ days for certificates
3. **Secure CA key**: Store CA private key securely, preferably in a hardware security module (HSM)
4. **Certificate rotation**: Plan for certificate renewal before expiration
5. **Revocation**: Implement certificate revocation lists (CRL) or OCSP
6. **Strong passwords**: Replace "changeit" with strong, unique passwords
7. **Vault integration**: Consider using HashiCorp Vault or AWS Certificate Manager
