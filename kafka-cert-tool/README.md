# Kafka Certificate Tool

A Java Spring Boot CLI application for generating CA and broker certificates for Apache Kafka.

## Features

- Generate self-signed CA certificates
- Generate wildcard broker certificates signed by CA
- Generate client certificates signed by CA
- Create JKS keystores and truststores
- Export certificates in PEM format

## Tech Stack

- Java 25
- Spring Boot 4.0.0
- Gradle Groovy build system
- BouncyCastle for certificate generation
- Picocli for CLI interface
- JUnit 5 + Mockito for testing
- JaCoCo for code coverage

## Prerequisites

- Java 25 or higher
- Gradle 8.x or higher

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Code Coverage

```bash
./gradlew jacocoTestReport
```

View the coverage report at `build/reports/jacoco/test/html/index.html`

## Usage

### Generate CA Certificate

```bash
./gradlew bootRun --args="generate-ca --output-dir ./certs --cn 'Kafka CA' --validity-days 3650"
```

Outputs:
- `ca-key.pem` - CA private key
- `ca-cert.pem` - CA certificate

### Generate Broker Certificate

```bash
./gradlew bootRun --args="generate-broker-cert --ca-cert ./certs/ca-cert.pem --ca-key ./certs/ca-key.pem --output-dir ./certs --domain '*.kafka.local' --validity-days 365 --keystore-password changeit"
```

Outputs:
- `broker-key.pem` - Broker private key
- `broker-cert.pem` - Broker certificate
- `broker.keystore.jks` - Broker keystore
- `broker.truststore.jks` - Broker truststore

### Generate Client Certificate

```bash
./gradlew bootRun --args="generate-client-cert --ca-cert ./certs/ca-cert.pem --ca-key ./certs/ca-key.pem --output-dir ./certs --cn 'client1' --validity-days 365 --keystore-password changeit"
```

Outputs:
- `client-key.pem` - Client private key
- `client-cert.pem` - Client certificate
- `client.keystore.jks` - Client keystore
- `client.truststore.jks` - Client truststore

## Command Options

### generate-ca

| Option | Description | Default |
|--------|-------------|---------|
| `--output-dir` | Output directory | `.` |
| `--cn` | Common Name | `Kafka CA` |
| `--validity-days` | Validity in days | `3650` |

### generate-broker-cert

| Option | Description | Default |
|--------|-------------|---------|
| `--ca-cert` | CA certificate file | Required |
| `--ca-key` | CA private key file | Required |
| `--output-dir` | Output directory | `.` |
| `--domain` | Domain name (supports wildcards) | `*.kafka.local` |
| `--validity-days` | Validity in days | `365` |
| `--keystore-password` | Keystore password | `changeit` |

### generate-client-cert

| Option | Description | Default |
|--------|-------------|---------|
| `--ca-cert` | CA certificate file | Required |
| `--ca-key` | CA private key file | Required |
| `--output-dir` | Output directory | `.` |
| `--cn` | Common Name (Kafka principal) | Required |
| `--validity-days` | Validity in days | `365` |
| `--keystore-password` | Keystore password | `changeit` |

## License

MIT
