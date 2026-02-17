# Agent: Helm Charts

## Objective
Update Schema Registry chart for mTLS to Kafka. Create Kafka UI and Prometheus Helm charts.

## Output Directory
`charts/`

## Requirements
1. Schema Registry: add mTLS support for connecting to Kafka brokers
2. Kafka UI: deploy kafka-ui (provectus/kafka-ui) with mTLS to Kafka
3. Prometheus: deploy with scrape configs targeting Kafka JMX exporter endpoints
