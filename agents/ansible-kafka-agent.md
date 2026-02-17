# Agent: Ansible Kafka Deployment

## Objective
Create Ansible roles and playbooks to deploy Apache Kafka brokers and controllers on dedicated VMs using KRaft mode with mTLS authentication and JMX metrics exposure.

## Output Directory
`ansible/`

## Requirements
1. KRaft mode (no ZooKeeper) - Apache Kafka 3.8+
2. mTLS authentication on all listeners with CN extraction as principal
3. JMX exporter agent (Prometheus JMX exporter) on each broker and controller
4. Separate roles for broker and controller (combined role possible)
5. Inventory structure supporting multiple environments
6. Keep configuration minimal - use Kafka defaults where possible

## Key Configuration
- `ssl.client.auth=required` for mTLS
- `ssl.principal.mapping.rules=RULE:^CN=(.*?),...$/$1/` to extract CN
- JMX exporter agent JAR with kafka-broker.yml metrics config
- KRaft: `process.roles=broker` or `process.roles=controller` or combined
