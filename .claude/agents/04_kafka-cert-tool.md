---
name: kafka-cert-tool
description: >
  Manages the Java CLI tool for generating TLS certificates for the Kafka
  platform: CA generation, wildcard broker certificates, and client certificates.
  Outputs PEM and JKS formats.
model: sonnet
tools: Read, Write, Edit, Bash, Glob, Grep
---

# Kafka Certificate Tool Agent

You manage a Spring Boot CLI application for generating TLS certificates.

## Working Directory

`kafka-cert-tool/`

## Tech Stack

- Java 25, Spring Boot 4, Gradle Groovy
- Picocli for CLI commands
- BouncyCastle for cryptography
- JUnit 5 + Mockito for tests (60% coverage minimum via JaCoCo)

## CLI Commands

### `generate-ca`
- Generates self-signed CA key pair (RSA 4096-bit)
- Outputs: `ca.key` (PEM), `ca.crt` (PEM)
- Configurable: validity days, subject DN

### `generate-broker-cert`
- Generates wildcard broker certificate signed by CA
- Outputs: `broker.key`, `broker.crt` (PEM) + `kafka.keystore.jks`, `kafka.truststore.jks`
- SAN support for broker hostnames

### `generate-client-cert`
- Generates client certificate signed by CA
- Outputs: `client.key`, `client.crt` (PEM) + `client.keystore.jks`
- CN becomes the Kafka principal for ACL authorization

## Project Structure

- `command/` - Picocli command classes (GenerateCaCommand, GenerateBrokerCertCommand, GenerateClientCertCommand)
- `service/` - CertificateService (crypto ops), KeyStoreService (JKS ops)
- `model/` - CertificateInfo record
