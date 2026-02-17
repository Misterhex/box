# Kafka KRaft Deployment - Summary

## What Was Created

Complete Ansible automation for deploying Apache Kafka 3.8.1 in KRaft mode (ZooKeeper-free) with mTLS security and JMX monitoring.

## Directory Structure

```
/home/user/box/ansible/
├── inventory/
│   └── hosts.yml                              # 3 controllers + 3 brokers
├── group_vars/
│   ├── all.yml                                # Common vars (Kafka version, SSL, JMX)
│   ├── kafka_brokers.yml                      # Broker-specific (heap, listeners)
│   └── kafka_controllers.yml                  # Controller-specific (heap, listeners)
├── roles/
│   ├── kafka-common/                          # Shared setup
│   │   ├── tasks/main.yml                     # Java, user, dirs, JMX, service
│   │   ├── handlers/main.yml                  # Systemd reload/restart
│   │   ├── templates/
│   │   │   ├── kafka.service.j2               # Systemd unit
│   │   │   └── jmx-exporter-config.yml.j2     # Prometheus JMX config
│   │   └── defaults/main.yml
│   ├── kafka-broker/                          # Broker deployment
│   │   ├── tasks/main.yml                     # Deploy + format + start
│   │   ├── templates/
│   │   │   └── server.properties.j2           # Broker config
│   │   └── defaults/main.yml
│   └── kafka-controller/                      # Controller deployment
│       ├── tasks/main.yml                     # Deploy + format + start
│       ├── templates/
│       │   └── server.properties.j2           # Controller config
│       └── defaults/main.yml
├── playbooks/
│   ├── site.yml                               # Full deployment
│   ├── deploy-controllers.yml                 # Controllers only
│   └── deploy-brokers.yml                     # Brokers only
├── README.md                                  # Full documentation
├── QUICKSTART.md                              # Quick reference
├── SSL_CERTIFICATE_SETUP.md                   # Certificate generation guide
└── DEPLOYMENT_SUMMARY.md                      # This file
```

## Key Features Implemented

### 1. KRaft Mode (No ZooKeeper)
- **Controllers**: 3 nodes running `process.roles=controller`
  - Node IDs: 1, 2, 3
  - Listener: `CONTROLLER://0.0.0.0:9094` (SSL)
  - Forms quorum: `1@controller-1:9094,2@controller-2:9094,3@controller-3:9094`

- **Brokers**: 3 nodes running `process.roles=broker`
  - Node IDs: 101, 102, 103
  - Listeners:
    - `BROKER://0.0.0.0:9092` (inter-broker, SSL)
    - `CLIENT://0.0.0.0:9093` (client connections, SSL)

### 2. mTLS Authentication
- **All listeners require SSL**: `listener.security.protocol.map=BROKER:SSL,CLIENT:SSL,CONTROLLER:SSL`
- **Client certificate validation**: `ssl.client.auth=required`
- **Principal mapping**: Extracts CN from certificates
  - Rule: `ssl.principal.mapping.rules=RULE:^CN=(.*?),.*$/$1/,DEFAULT`
- **Certificate locations**:
  - Keystore: `/etc/kafka/ssl/kafka.keystore.jks`
  - Truststore: `/etc/kafka/ssl/kafka.truststore.jks`

### 3. Authorization
- **Authorizer**: `org.apache.kafka.metadata.authorizer.StandardAuthorizer`
- **Super users**: `User:kafka-broker`
- **Default policy**: Deny (explicit ACLs required)

### 4. JMX Metrics Exposure
- **JMX Exporter**: Prometheus javaagent on port 7071
- **Metrics covered**:
  - Kafka server metrics (broker, network, controller)
  - Log metrics (per-topic, per-partition)
  - JVM metrics (heap, GC, threads)
- **Configuration**: `/opt/kafka/kafka_2.13-3.8.1/config/jmx-exporter-config.yml`
- **Endpoint**: `http://<node>:7071/metrics`

### 5. Production-Ready Setup
- **Systemd service**: Auto-start, restart on failure
- **Proper user/permissions**: Dedicated `kafka` user and group
- **Storage formatting**: Automated with `kafka-storage.sh`
- **Log directories**:
  - Controllers: `/var/lib/kafka/controller-logs`
  - Brokers: `/var/lib/kafka/broker-logs`

## Configuration Highlights

### Broker server.properties
```properties
node.id=101-103
process.roles=broker
controller.quorum.voters=1@controller-1:9094,2@controller-2:9094,3@controller-3:9094
listeners=BROKER://:9092,CLIENT://:9093
advertised.listeners=BROKER://{{ hostname }}:9092,CLIENT://{{ hostname }}:9093
listener.security.protocol.map=BROKER:SSL,CLIENT:SSL,CONTROLLER:SSL
inter.broker.listener.name=BROKER

# mTLS
ssl.keystore.location=/etc/kafka/ssl/kafka.keystore.jks
ssl.truststore.location=/etc/kafka/ssl/kafka.truststore.jks
ssl.client.auth=required
ssl.principal.mapping.rules=RULE:^CN=(.*?),.*$/$1/,DEFAULT

# Authorization
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
super.users=User:kafka-broker
allow.everyone.if.no.acl.found=false
```

### Controller server.properties
```properties
node.id=1-3
process.roles=controller
controller.quorum.voters=1@controller-1:9094,2@controller-2:9094,3@controller-3:9094
listeners=CONTROLLER://:9094
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:SSL

# mTLS (same as brokers)
ssl.keystore.location=/etc/kafka/ssl/kafka.keystore.jks
ssl.truststore.location=/etc/kafka/ssl/kafka.truststore.jks
ssl.client.auth=required
ssl.principal.mapping.rules=RULE:^CN=(.*?),.*$/$1/,DEFAULT
```

### Systemd Service
```ini
[Unit]
Description=Apache Kafka (KRaft mode)
After=network.target

[Service]
Type=simple
User=kafka
Environment="KAFKA_HEAP_OPTS=-Xms2G -Xmx2G"  # Brokers: 2G, Controllers: 1G
Environment="KAFKA_JMX_OPTS=-javaagent:/opt/kafka/jmx_prometheus_javaagent-1.0.1.jar=7071:..."
ExecStart=/opt/kafka/kafka_2.13-3.8.1/bin/kafka-server-start.sh /opt/kafka/kafka_2.13-3.8.1/config/server.properties
Restart=on-failure
LimitNOFILE=100000

[Install]
WantedBy=multi-user.target
```

## Pre-Deployment Requirements

### 1. SSL Certificates (CRITICAL)
Each node needs:
- `/etc/kafka/ssl/kafka.keystore.jks` - Node's private key and certificate
- `/etc/kafka/ssl/kafka.truststore.jks` - CA certificate to validate peers

See `SSL_CERTIFICATE_SETUP.md` for detailed instructions.

### 2. Inventory Configuration
Update `/home/user/box/ansible/inventory/hosts.yml` with actual:
- Hostnames (must be DNS resolvable or in /etc/hosts)
- IP addresses

### 3. Cluster ID
Generate a unique cluster ID:
```bash
docker run --rm apache/kafka:3.8.1 kafka-storage random-uuid
```
Update `kafka_cluster_id` in `group_vars/all.yml`.

### 4. Passwords (Production)
Change default passwords in `group_vars/all.yml`:
- `kafka_keystore_password`
- `kafka_truststore_password`

Consider using Ansible Vault for production.

## Deployment Commands

### Full deployment
```bash
cd /home/user/box/ansible
ansible-playbook -i inventory/hosts.yml playbooks/site.yml
```

### Controllers only
```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-controllers.yml
```

### Brokers only
```bash
ansible-playbook -i inventory/hosts.yml playbooks/deploy-brokers.yml
```

## Post-Deployment Validation

### 1. Check services
```bash
ansible all -i inventory/hosts.yml -m shell -a "systemctl status kafka" -b
```

### 2. View logs
```bash
ansible all -i inventory/hosts.yml -m shell -a "journalctl -u kafka -n 50" -b
```

### 3. Test JMX metrics
```bash
curl http://broker-1:7071/metrics | head -20
curl http://controller-1:7071/metrics | grep kafka_controller
```

### 4. Create test topic
```bash
# On any broker (requires client-ssl.properties)
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9093 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 3 \
  --command-config /tmp/client-ssl.properties
```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Kafka KRaft Cluster                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Controllers (Metadata Quorum)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ controller-1 │  │ controller-2 │  │ controller-3 │     │
│  │   node.id=1  │  │   node.id=2  │  │   node.id=3  │     │
│  │ :9094 (SSL)  │  │ :9094 (SSL)  │  │ :9094 (SSL)  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         └──────────────────┴──────────────────┘             │
│                   KRaft Consensus                           │
│                         │                                   │
│                         │ Metadata                          │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Brokers                          │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐         │   │
│  │  │ broker-1 │  │ broker-2 │  │ broker-3 │         │   │
│  │  │ node=101 │  │ node=102 │  │ node=103 │         │   │
│  │  │ :9092 ◄──┼──► :9092 ◄──┼──► :9092    │ (inter) │   │
│  │  │ :9093    │  │ :9093    │  │ :9093    │ (client)│   │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘         │   │
│  └───────┼─────────────┼─────────────┼───────────────┘   │
│          │             │             │                    │
└──────────┼─────────────┼─────────────┼────────────────────┘
           │             │             │
           ▼             ▼             ▼
      Clients (mTLS required on all connections)

JMX Metrics: Each node exposes Prometheus metrics on :7071
```

## Security Model

1. **All network communication encrypted**: SSL on all listeners
2. **Mutual authentication**: Both client and server certificates validated
3. **Identity extraction**: CN from certificate becomes Kafka principal
4. **Authorization**: ACLs enforced (default deny)
5. **Super users**: `kafka-broker` principal has full access

## Monitoring Integration

### Prometheus scrape config
```yaml
scrape_configs:
  - job_name: 'kafka-controllers'
    static_configs:
      - targets: ['controller-1:7071', 'controller-2:7071', 'controller-3:7071']
  - job_name: 'kafka-brokers'
    static_configs:
      - targets: ['broker-1:7071', 'broker-2:7071', 'broker-3:7071']
```

### Key metrics to monitor
- `kafka_server_ReplicaManager_UnderReplicatedPartitions`
- `kafka_controller_KafkaController_ActiveControllerCount`
- `kafka_server_BrokerTopicMetrics_MessagesInPerSec`
- `kafka_network_RequestMetrics_TotalTimeMs`
- `jvm_memory_heap_used`
- `jvm_gc_collection_time_ms`

## Troubleshooting

See `QUICKSTART.md` for common issues and solutions.

## Next Steps

1. **Provision SSL certificates** (see `SSL_CERTIFICATE_SETUP.md`)
2. **Update inventory** with your node IPs
3. **Generate cluster ID** and update `group_vars/all.yml`
4. **Run deployment** with `ansible-playbook`
5. **Configure ACLs** for your applications
6. **Set up monitoring** with Prometheus/Grafana
7. **Test thoroughly** before production use

## Files Overview

| File | Purpose |
|------|---------|
| `inventory/hosts.yml` | Node definitions and IPs |
| `group_vars/all.yml` | Common settings (version, SSL, JMX) |
| `group_vars/kafka_brokers.yml` | Broker-specific settings |
| `group_vars/kafka_controllers.yml` | Controller-specific settings |
| `roles/kafka-common/` | Shared setup (Java, user, JMX, systemd) |
| `roles/kafka-broker/` | Broker configuration and deployment |
| `roles/kafka-controller/` | Controller configuration and deployment |
| `playbooks/site.yml` | Full deployment playbook |
| `playbooks/deploy-brokers.yml` | Broker-only playbook |
| `playbooks/deploy-controllers.yml` | Controller-only playbook |
| `README.md` | Complete documentation |
| `QUICKSTART.md` | Quick reference guide |
| `SSL_CERTIFICATE_SETUP.md` | Certificate generation guide |

## Production Checklist

- [ ] SSL certificates generated and distributed
- [ ] Inventory updated with production IPs/hostnames
- [ ] Unique cluster ID generated
- [ ] Strong passwords set (not "changeit")
- [ ] Ansible Vault used for sensitive variables
- [ ] Firewall rules configured (9092-9094, 7071)
- [ ] DNS or /etc/hosts configured for hostnames
- [ ] Prometheus configured to scrape JMX metrics
- [ ] Backup strategy for `/var/lib/kafka` data
- [ ] Monitoring alerts configured
- [ ] ACLs planned and documented
- [ ] Disaster recovery procedures documented
- [ ] Certificate rotation plan in place
