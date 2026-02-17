# Main Agent (Opus) - Kafka mTLS Platform Orchestrator

## Role
Plans and coordinates all sub-agents. Runs on Opus model.

## Platform Architecture

```
+-------------------+     +-------------------+     +-------------------+
|  Kafka Broker 1   |     |  Kafka Broker 2   |     |  Kafka Broker 3   |
|  (VM - Ansible)   |     |  (VM - Ansible)   |     |  (VM - Ansible)   |
|  mTLS + JMX       |     |  mTLS + JMX       |     |  mTLS + JMX       |
+-------------------+     +-------------------+     +-------------------+
         |                         |                         |
         +-------------------------+-------------------------+
                                   |
                            mTLS (CN=principal)
                                   |
    +------------------+  +------------------+  +------------------+
    | Schema Registry  |  |    Kafka UI      |  |   Prometheus     |
    | (K8s - Helm)     |  |  (K8s - Helm)    |  |  (K8s - Helm)    |
    +------------------+  +------------------+  +------------------+
                                   |
    +------------------+  +------------------+  +------------------+
    |  Cert Tool CLI   |  |  Admin API       |  | Synthetic Client |
    | (Java/Spring)    |  | (Java/Spring)    |  | (Java/Spring)    |
    +------------------+  +------------------+  +------------------+
```

## Sub-Agents (all use Sonnet)

| Agent | Directory | Responsibility |
|-------|-----------|---------------|
| ansible-kafka | `ansible/` | Ansible roles/playbooks for Kafka brokers+controllers, mTLS, JMX |
| helm-charts | `charts/` | Schema Registry mTLS update, Kafka UI chart, Prometheus chart |
| cert-tool | `kafka-cert-tool/` | Spring Boot CLI to generate CA + wildcard certs |
| admin-api | `kafka-admin-api/` | REST API wrapping KafkaAdminClient |
| synthetic-client | `kafka-synthetic-client/` | mTLS producer/consumer with Avro schema |

## Conventions
- KISS: minimal config, use defaults
- Java 25, Spring Boot 4, Gradle Groovy
- Unit tests with 60% code coverage per class
- mTLS everywhere
- Apache Kafka (KRaft mode, no ZooKeeper)
