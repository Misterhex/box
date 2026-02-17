# Kafka KRaft Deployment with Ansible

Complete Ansible deployment for Apache Kafka 3.8.1 in KRaft mode (ZooKeeper-free) with mTLS authentication and JMX metrics exposure.

## Features

- **KRaft Mode**: No ZooKeeper dependency, using Kafka's native metadata quorum
- **Separate Controllers & Brokers**: Dedicated controller nodes for cluster metadata
- **mTLS Authentication**: Mutual TLS on all listeners with client certificate validation
- **JMX Metrics**: Prometheus JMX Exporter on port 7071 for monitoring
- **Production-Ready**: Systemd services, proper security, and organized structure

## Architecture

- **Controllers** (3 nodes): Manage cluster metadata via KRaft consensus
  - Listener: `CONTROLLER://0.0.0.0:9094` (SSL)
  - Process role: `controller`

- **Brokers** (3 nodes): Handle client requests and data replication
  - Inter-broker: `BROKER://0.0.0.0:9092` (SSL)
  - Client: `CLIENT://0.0.0.0:9093` (SSL)
  - Process role: `broker`

## Prerequisites

### SSL Certificates

Before running the playbooks, you **must** provision SSL certificates on each node:

1. **Keystore** (`/etc/kafka/ssl/kafka.keystore.jks`): Contains the node's private key and certificate
2. **Truststore** (`/etc/kafka/ssl/kafka.truststore.jks`): Contains trusted CA certificates

Example certificate generation (adjust for your PKI):

```bash
# Generate CA
openssl req -new -x509 -keyout ca-key -out ca-cert -days 365

# For each node, generate key and certificate
keytool -keystore kafka.keystore.jks -alias kafka -validity 365 -genkey -keyalg RSA
keytool -keystore kafka.keystore.jks -alias kafka -certreq -file cert-file
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial
keytool -keystore kafka.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.keystore.jks -alias kafka -import -file cert-signed

# Create truststore with CA
keytool -keystore kafka.truststore.jks -alias CARoot -import -file ca-cert
```

### Ansible Requirements

- Ansible 2.9 or higher
- SSH access to all nodes with sudo privileges
- Python 3 on target nodes

## Directory Structure

```
ansible/
├── inventory/
│   └── hosts.yml                    # Inventory with node definitions
├── group_vars/
│   ├── all.yml                      # Common variables
│   ├── kafka_brokers.yml            # Broker-specific variables
│   └── kafka_controllers.yml        # Controller-specific variables
├── roles/
│   ├── kafka-common/                # Common setup (Java, user, certs, JMX)
│   ├── kafka-broker/                # Broker configuration
│   └── kafka-controller/            # Controller configuration
└── playbooks/
    ├── site.yml                     # Deploy everything
    ├── deploy-controllers.yml       # Controllers only
    └── deploy-brokers.yml           # Brokers only
```

## Usage

### 1. Update Inventory

Edit `inventory/hosts.yml` with your actual hostnames and IP addresses:

```yaml
all:
  children:
    kafka_controllers:
      hosts:
        controller-1:
          kafka_node_id: 1
          ansible_host: 10.0.1.1
        # ... more controllers
    kafka_brokers:
      hosts:
        broker-1:
          kafka_node_id: 101
          ansible_host: 10.0.2.1
        # ... more brokers
```

### 2. Configure Variables

Review and adjust variables in `group_vars/`:

- `all.yml`: Kafka version, SSL passwords, cluster ID
- `kafka_brokers.yml`: Broker heap size, listener configuration
- `kafka_controllers.yml`: Controller heap size, listener configuration

**Important**: Generate a new cluster ID:

```bash
docker run --rm apache/kafka:3.8.1 kafka-storage random-uuid
# Or use kafka-storage.sh from a Kafka installation
```

Update `kafka_cluster_id` in `group_vars/all.yml`.

### 3. Deploy

Deploy everything (controllers first, then brokers):

```bash
ansible-playbook -i inventory/hosts.yml playbooks/site.yml
```

Deploy only controllers:

```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-controllers.yml
```

Deploy only brokers:

```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-brokers.yml
```

### 4. Verify Deployment

Check service status:

```bash
ansible all -i inventory/hosts.yml -m shell -a "systemctl status kafka" -b
```

View logs:

```bash
ansible all -i inventory/hosts.yml -m shell -a "journalctl -u kafka -n 50" -b
```

Test JMX metrics endpoint:

```bash
curl http://<node-ip>:7071/metrics
```

### 5. Create Topics and Test

From any broker node:

```bash
# Create a topic (requires proper SSL client configuration)
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --create \
  --bootstrap-server broker-1:9093 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 3 \
  --command-config /etc/kafka/client-ssl.properties

# List topics
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --list \
  --bootstrap-server broker-1:9093 \
  --command-config /etc/kafka/client-ssl.properties
```

Client SSL configuration example (`client-ssl.properties`):

```properties
security.protocol=SSL
ssl.truststore.location=/etc/kafka/ssl/kafka.truststore.jks
ssl.truststore.password=changeit
ssl.keystore.location=/etc/kafka/ssl/kafka.keystore.jks
ssl.keystore.password=changeit
```

## Configuration Details

### mTLS Security

All communication requires mutual TLS authentication:

- `ssl.client.auth=required`: Enforces client certificate validation
- Principal extraction: `ssl.principal.mapping.rules=RULE:^CN=(.*?),.*$/$1/,DEFAULT`
- Authorization: `StandardAuthorizer` with ACLs (default deny)

### JMX Metrics

Prometheus JMX Exporter exposes Kafka metrics on port 7071:

- Kafka server metrics (broker, network, controller)
- Log metrics (per-topic, per-partition)
- JVM metrics (heap, GC, threads)

Scrape endpoint: `http://<node-ip>:7071/metrics`

### Log Directories

- **Controllers**: `/var/lib/kafka/controller-logs`
- **Brokers**: `/var/lib/kafka/broker-logs`

### Systemd Service

Kafka runs as a systemd service:

```bash
systemctl status kafka
systemctl restart kafka
journalctl -u kafka -f
```

## Scaling

### Add Brokers

1. Add new broker entries to `inventory/hosts.yml` with unique `kafka_node_id`
2. Run broker deployment:
   ```bash
   ansible-playbook -i inventory/hosts.yml playbooks/deploy-brokers.yml --limit new-broker-hostname
   ```

### Add Controllers

Controllers use quorum voting, so changes require:

1. Add new controller to inventory
2. Update `kafka_controller_quorum_voters` in group_vars
3. Follow Kafka documentation for adding voters to existing quorum

## Troubleshooting

### SSL Certificate Issues

Check keystore contents:
```bash
keytool -list -v -keystore /etc/kafka/ssl/kafka.keystore.jks
```

### Controller Connection Issues

Verify controller quorum:
```bash
# From any broker
grep "controller.quorum.voters" /opt/kafka/*/config/server.properties
```

### Service Won't Start

Check logs:
```bash
journalctl -u kafka -n 100 --no-pager
```

Check storage format:
```bash
ls -la /var/lib/kafka/*/meta.properties
```

## Security Considerations

1. **Change default passwords**: Update `kafka_keystore_password` and `kafka_truststore_password` in production
2. **Secure certificate management**: Use Ansible Vault for sensitive variables
3. **Configure ACLs**: The default is deny-all; configure appropriate ACLs for clients
4. **Firewall**: Restrict access to Kafka ports (9092-9094, 7071)

## License

This deployment configuration is provided as-is for use with Apache Kafka.
