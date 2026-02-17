# Agent: Certificate Tool CLI

## Objective
Java Spring Boot CLI application to generate CA and wildcard broker certificates for the Kafka platform.

## Output Directory
`kafka-cert-tool/`

## Tech Stack
- Java 25, Spring Boot 4, Gradle Groovy
- Unit tests, 60% code coverage per class

## Features
- Generate self-signed CA (key + cert)
- Generate wildcard broker certificates signed by CA
- Generate client certificates signed by CA
- Output PEM and JKS/PKCS12 formats
- Simple CLI interface using Spring Boot CLI runner
