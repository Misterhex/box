# Quick Start Guide

## Pre-Deployment Checklist

1. **Update Inventory** (`inventory/hosts.yml`):
   - Replace placeholder IPs with your actual node IPs
   - Ensure hostnames are resolvable or use IPs directly

2. **Generate Cluster ID**:
   ```bash
   # Use Kafka's built-in tool
   docker run --rm apache/kafka:3.8.1 kafka-storage random-uuid
   ```
   Update `kafka_cluster_id` in `group_vars/all.yml`

3. **Provision SSL Certificates** (REQUIRED):
   - Each node needs `/etc/kafka/ssl/kafka.keystore.jks`
   - Each node needs `/etc/kafka/ssl/kafka.truststore.jks`
   - Update passwords in `group_vars/all.yml` if not using defaults

4. **Update Passwords** (Recommended):
   - Change `kafka_keystore_password` and `kafka_truststore_password` in `group_vars/all.yml`
   - Consider using Ansible Vault for production:
     ```bash
     ansible-vault encrypt_string 'your-password' --name 'kafka_keystore_password'
     ```

## Deployment Commands

### Full Deployment (Controllers + Brokers)

```bash
cd /home/user/box/ansible
ansible-playbook -i inventory/hosts.yml playbooks/site.yml
```

### Deploy Controllers Only

```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-controllers.yml
```

### Deploy Brokers Only

```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-brokers.yml
```

### Deploy to Specific Nodes

```bash
# Single node
ansible-playbook -i inventory/hosts.yml playbooks/site.yml --limit controller-1

# Multiple nodes
ansible-playbook -i inventory/hosts.yml playbooks/site.yml --limit "broker-1,broker-2"
```

## Post-Deployment Verification

### Check Service Status

```bash
# All nodes
ansible all -i inventory/hosts.yml -m shell -a "systemctl status kafka" -b

# Controllers only
ansible kafka_controllers -i inventory/hosts.yml -m shell -a "systemctl status kafka" -b

# Brokers only
ansible kafka_brokers -i inventory/hosts.yml -m shell -a "systemctl status kafka" -b
```

### View Logs

```bash
# Last 50 lines from all nodes
ansible all -i inventory/hosts.yml -m shell -a "journalctl -u kafka -n 50 --no-pager" -b

# Follow logs on specific node
ssh broker-1 "journalctl -u kafka -f"
```

### Test JMX Metrics

```bash
# From your local machine
curl http://broker-1:7071/metrics | head -20

# From controller
curl http://controller-1:7071/metrics | grep kafka_controller
```

### Verify Cluster Formation

```bash
# SSH to any broker
ssh broker-1

# Check metadata
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-metadata.sh --snapshot \
  /var/lib/kafka/broker-logs/__cluster_metadata-0/*.checkpoint \
  --print > /tmp/metadata.txt
```

## Common Operations

### Restart Kafka on All Nodes

```bash
ansible all -i inventory/hosts.yml -m systemd -a "name=kafka state=restarted" -b
```

### Update Configuration

```bash
# 1. Edit templates in roles/kafka-*/templates/
# 2. Run playbook to update
ansible-playbook -i inventory/hosts.yml playbooks/site.yml

# 3. Restart services
ansible all -i inventory/hosts.yml -m systemd -a "name=kafka state=restarted" -b
```

### Create a Test Topic

First, create a client SSL properties file on a broker:

```bash
cat > /tmp/client-ssl.properties <<EOF
security.protocol=SSL
ssl.truststore.location=/etc/kafka/ssl/kafka.truststore.jks
ssl.truststore.password=changeit
ssl.keystore.location=/etc/kafka/ssl/kafka.keystore.jks
ssl.keystore.password=changeit
ssl.key.password=changeit
EOF
```

Then create and test the topic:

```bash
# Create topic
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9093 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 3 \
  --command-config /tmp/client-ssl.properties

# List topics
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --list \
  --bootstrap-server localhost:9093 \
  --command-config /tmp/client-ssl.properties

# Describe topic
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --describe \
  --bootstrap-server localhost:9093 \
  --topic test-topic \
  --command-config /tmp/client-ssl.properties
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
journalctl -u kafka -n 100 --no-pager

# Check if storage is formatted
ls -la /var/lib/kafka/*/meta.properties

# Check configuration
cat /opt/kafka/kafka_2.13-3.8.1/config/server.properties | grep -v ^# | grep -v ^$
```

### SSL Connection Errors

```bash
# Verify keystore
keytool -list -v -keystore /etc/kafka/ssl/kafka.keystore.jks -storepass changeit

# Verify truststore
keytool -list -v -keystore /etc/kafka/ssl/kafka.truststore.jks -storepass changeit

# Test SSL connection
openssl s_client -connect broker-1:9093 -CAfile /path/to/ca-cert
```

### Controller Quorum Issues

```bash
# Check quorum configuration on all nodes
ansible all -i inventory/hosts.yml -m shell \
  -a "grep 'controller.quorum.voters' /opt/kafka/*/config/server.properties" -b

# Verify controller logs
journalctl -u kafka -n 200 --no-pager | grep -i controller
```

## Performance Tuning

### Adjust JVM Heap Size

Edit `group_vars/kafka_brokers.yml` or `group_vars/kafka_controllers.yml`:

```yaml
# For brokers (default: 2G)
kafka_heap_opts: "-Xms4G -Xmx4G"

# For controllers (default: 1G)
kafka_heap_opts: "-Xms2G -Xmx2G"
```

Then re-run the playbook and restart services.

### Monitor JMX Metrics

Add these targets to your Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'kafka-controllers'
    static_configs:
      - targets:
        - controller-1:7071
        - controller-2:7071
        - controller-3:7071
    labels:
      cluster: 'kafka-kraft'
      role: 'controller'

  - job_name: 'kafka-brokers'
    static_configs:
      - targets:
        - broker-1:7071
        - broker-2:7071
        - broker-3:7071
    labels:
      cluster: 'kafka-kraft'
      role: 'broker'
```

## Directory Structure

```
/opt/kafka/kafka_2.13-3.8.1/          # Kafka installation
/etc/kafka/ssl/                        # SSL certificates
/var/lib/kafka/broker-logs/            # Broker data (on broker nodes)
/var/lib/kafka/controller-logs/        # Controller data (on controller nodes)
/var/lib/kafka/logs/                   # Application logs
/etc/systemd/system/kafka.service      # Systemd unit file
```
