# Agent: Kafka Admin REST API

## Objective
REST API wrapping KafkaAdminClient for simple operations.

## Output Directory
`kafka-admin-api/`

## Tech Stack
- Java 25, Spring Boot 4, Gradle Groovy
- Unit tests, 60% code coverage per class

## Features
- Query cluster info
- Create/list/describe/delete topics
- Manage ACLs (create, list, delete)
- mTLS authentication to Kafka
- Simple REST endpoints
