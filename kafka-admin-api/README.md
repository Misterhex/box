# Kafka Admin API

A Spring Boot REST API application that wraps Apache Kafka's AdminClient to provide easy-to-use endpoints for managing Kafka clusters, topics, and ACLs.

## Tech Stack

- Java 25
- Spring Boot 4.0.0
- Gradle (Groovy)
- Apache Kafka Clients 3.8.1
- JUnit 5 + Mockito
- JaCoCo (Code Coverage)

## Prerequisites

- Java 25 JDK
- Gradle
- Apache Kafka cluster (with SSL/mTLS configured)

## Configuration

Update `src/main/resources/application.properties` with your Kafka cluster details:

```properties
kafka.bootstrap-servers=localhost:9093
kafka.ssl.keystore-location=/etc/kafka/ssl/client.keystore.jks
kafka.ssl.keystore-password=changeit
kafka.ssl.truststore-location=/etc/kafka/ssl/client.truststore.jks
kafka.ssl.truststore-password=changeit
server.port=8080
```

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Code Coverage

Generate code coverage report:

```bash
./gradlew jacocoTestReport
```

View the report at: `build/reports/jacoco/test/html/index.html`

Verify coverage (minimum 60%):

```bash
./gradlew jacocoTestCoverageVerification
```

## Running the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### Cluster Management

- `GET /api/cluster` - Get cluster information (ID, nodes)

### Topic Management

- `GET /api/topics` - List all topics
- `GET /api/topics/{name}` - Get topic details
- `POST /api/topics` - Create a new topic
  ```json
  {
    "name": "my-topic",
    "partitions": 3,
    "replicationFactor": 3
  }
  ```
- `DELETE /api/topics/{name}` - Delete a topic

### ACL Management

- `GET /api/acls` - List all ACLs
- `POST /api/acls` - Create a new ACL
  ```json
  {
    "principal": "User:alice",
    "resourceType": "TOPIC",
    "resourceName": "my-topic",
    "operation": "READ",
    "permission": "ALLOW"
  }
  ```
- `DELETE /api/acls?resourceType=TOPIC&resourceName=my-topic&principal=User:alice&operation=READ` - Delete ACLs (all params optional)

## Example Usage

### List all topics
```bash
curl http://localhost:8080/api/topics
```

### Create a topic
```bash
curl -X POST http://localhost:8080/api/topics \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-topic",
    "partitions": 3,
    "replicationFactor": 2
  }'
```

### Get cluster information
```bash
curl http://localhost:8080/api/cluster
```

### Create an ACL
```bash
curl -X POST http://localhost:8080/api/acls \
  -H "Content-Type: application/json" \
  -d '{
    "principal": "User:alice",
    "resourceType": "TOPIC",
    "resourceName": "test-topic",
    "operation": "READ",
    "permission": "ALLOW"
  }'
```

## Project Structure

```
kafka-admin-api/
├── build.gradle
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/kafka/adminapi/
    │   │       ├── AdminApiApplication.java
    │   │       ├── config/
    │   │       │   └── KafkaAdminConfig.java
    │   │       ├── controller/
    │   │       │   ├── ClusterController.java
    │   │       │   ├── TopicController.java
    │   │       │   └── AclController.java
    │   │       ├── service/
    │   │       │   ├── ClusterService.java
    │   │       │   ├── TopicService.java
    │   │       │   └── AclService.java
    │   │       └── model/
    │   │           ├── ClusterInfo.java
    │   │           ├── NodeInfo.java
    │   │           ├── TopicInfo.java
    │   │           ├── CreateTopicRequest.java
    │   │           ├── AclBindingInfo.java
    │   │           └── CreateAclRequest.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/
            └── com/kafka/adminapi/
                ├── controller/
                │   ├── ClusterControllerTest.java
                │   ├── TopicControllerTest.java
                │   └── AclControllerTest.java
                └── service/
                    ├── ClusterServiceTest.java
                    ├── TopicServiceTest.java
                    └── AclServiceTest.java
```
