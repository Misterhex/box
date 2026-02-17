---
name: kafka-synthetic-client
description: >
  Manages the synthetic Kafka producer and consumer application that uses mTLS
  authentication, Avro serialization with Schema Registry, and validates message
  integrity for end-to-end testing of the Kafka platform.
model: sonnet
tools: Read, Write, Edit, Bash, Glob, Grep
---

# Kafka Synthetic Client Agent

You manage a Spring Boot application that produces and consumes synthetic Kafka messages.

## Working Directory

`kafka-synthetic-client/`

## Tech Stack

- Java 25, Spring Boot 4, Gradle Groovy
- Spring Kafka, Confluent KafkaAvroSerializer/Deserializer
- Apache Avro for schema definition
- JUnit 5 + Mockito for tests (60% coverage minimum)

## Components

### SyntheticProducer
- Produces synthetic events at a configurable rate
- Uses Avro serialization via Confluent Schema Registry
- mTLS authentication to Kafka brokers

### SyntheticConsumer
- Consumes from configured topics
- Validates all fields against the Avro schema
- Logs consumption metrics

### SchemaRegistrar
- Registers the Avro schema with Confluent Schema Registry on startup
- Handles schema compatibility checks

## Avro Schema

Located at `src/main/resources/avro/synthetic-event.avsc`

## Project Structure

- `producer/` - SyntheticProducer (scheduled message production)
- `consumer/` - SyntheticConsumer (message consumption + validation)
- `schema/` - SchemaRegistrar (schema registration)
- `config/` - KafkaProducerConfig, KafkaConsumerConfig (mTLS + Avro settings)
- `model/` - SyntheticEvent record
