# Agent: Synthetic Kafka Client

## Objective
Synthetic Kafka producer and consumer using mTLS, with Avro schema registration and data validation.

## Output Directory
`kafka-synthetic-client/`

## Tech Stack
- Java 25, Spring Boot 4, Gradle Groovy
- Unit tests, 60% code coverage per class

## Features
- Producer: registers Avro schema to Schema Registry, produces synthetic messages
- Consumer: consumes from topics, validates data against schema
- mTLS authentication to Kafka
- Configurable via application properties
