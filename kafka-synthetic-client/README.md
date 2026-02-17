# Kafka Synthetic Client

A Spring Boot application for producing and consuming synthetic Avro messages to/from Apache Kafka with mTLS support.

## Features

- **Synthetic Producer**: Generates and publishes Avro-encoded messages at a configurable rate
- **Synthetic Consumer**: Consumes and validates Avro messages
- **mTLS Support**: Secure communication with Kafka using mutual TLS
- **Avro Schema**: Type-safe message serialization with Confluent Schema Registry
- **Schema Registration**: Automatic schema registration on startup
- **Comprehensive Testing**: Unit tests with 60%+ code coverage using JUnit 5 and Mockito

## Tech Stack

- Java 25
- Spring Boot 4.0.0
- Gradle
- Apache Kafka 3.8.1
- Confluent Avro Serializer 7.6.0
- JUnit 5 + Mockito
- JaCoCo for code coverage

## Project Structure

```
kafka-synthetic-client/
  ├── build.gradle                           # Gradle build configuration
  ├── settings.gradle                        # Gradle settings
  └── src/
      ├── main/
      │   ├── java/com/kafka/synthetic/
      │   │   ├── SyntheticClientApplication.java    # Main application
      │   │   ├── config/
      │   │   │   ├── KafkaProducerConfig.java       # Producer configuration
      │   │   │   └── KafkaConsumerConfig.java       # Consumer configuration
      │   │   ├── producer/
      │   │   │   └── SyntheticProducer.java         # Message producer
      │   │   ├── consumer/
      │   │   │   └── SyntheticConsumer.java         # Message consumer
      │   │   ├── model/
      │   │   │   └── SyntheticEvent.java            # Event record
      │   │   └── schema/
      │   │       └── SchemaRegistrar.java           # Schema registration
      │   └── resources/
      │       ├── application.properties             # Application configuration
      │       └── avro/
      │           └── synthetic-event.avsc           # Avro schema
      └── test/
          └── java/com/kafka/synthetic/
              ├── producer/
              │   └── SyntheticProducerTest.java
              ├── consumer/
              │   └── SyntheticConsumerTest.java
              ├── schema/
              │   └── SchemaRegistrarTest.java
              └── config/
                  ├── KafkaProducerConfigTest.java
                  └── KafkaConsumerConfigTest.java
```

## Configuration

Edit `src/main/resources/application.properties` to configure:

```properties
# Kafka brokers
kafka.bootstrap-servers=localhost:9093

# Topic and consumer group
kafka.topic=synthetic-events
kafka.group-id=synthetic-consumer-group

# mTLS certificates
kafka.ssl.keystore-location=/etc/kafka/ssl/client.keystore.jks
kafka.ssl.keystore-password=changeit
kafka.ssl.truststore-location=/etc/kafka/ssl/client.truststore.jks
kafka.ssl.truststore-password=changeit

# Schema Registry
kafka.schema-registry.url=http://localhost:8081

# Producer rate (messages per second)
kafka.producer.rate-per-second=1

# Consumer offset reset
kafka.consumer.auto-offset-reset=earliest
```

## Build

```bash
./gradlew build
```

## Run Tests

```bash
./gradlew test
```

## Generate Code Coverage Report

```bash
./gradlew jacocoTestReport
```

Coverage report will be available at `build/reports/jacoco/test/html/index.html`

## Verify Code Coverage

```bash
./gradlew jacocoTestCoverageVerification
```

## Run Application

```bash
./gradlew bootRun
```

Or build and run the JAR:

```bash
./gradlew bootJar
java -jar build/libs/kafka-synthetic-client-0.1.0.jar
```

## Avro Schema

The application uses the following Avro schema for synthetic events:

```json
{
  "type": "record",
  "name": "SyntheticEvent",
  "namespace": "com.kafka.synthetic.avro",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "timestamp", "type": "long"},
    {"name": "source", "type": "string"},
    {"name": "payload", "type": "string"}
  ]
}
```

## How It Works

1. **Schema Registration**: On startup, the `SchemaRegistrar` reads the Avro schema and registers it with the Schema Registry.

2. **Producer**: The `SyntheticProducer` generates synthetic events at a configured rate (default: 1 per second). Each event contains:
   - Unique ID (UUID)
   - Current timestamp
   - Source identifier
   - Random payload data

3. **Consumer**: The `SyntheticConsumer` polls for messages, validates them, and logs the results. Validation checks:
   - All required fields are present
   - Field values are non-empty
   - Timestamp is valid

## mTLS Setup

To use mTLS, you need to generate client certificates and configure the keystore/truststore locations. Example:

```bash
# Generate client keystore
keytool -genkey -keystore client.keystore.jks -alias client -keyalg RSA

# Import CA certificate to truststore
keytool -import -file ca-cert.pem -keystore client.truststore.jks -alias ca-root
```

## License

MIT
