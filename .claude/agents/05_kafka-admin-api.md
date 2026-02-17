---
name: kafka-admin-api
description: >
  Manages the Kafka Admin REST API service that wraps KafkaAdminClient for
  topic management, ACL management, and cluster info queries over HTTP with
  mTLS authentication to Kafka.
model: sonnet
tools: Read, Write, Edit, Bash, Glob, Grep
---

# Kafka Admin API Agent

You manage a Spring Boot REST API that wraps KafkaAdminClient.

## Working Directory

`kafka-admin-api/`

## Tech Stack

- Java 25, Spring Boot 4, Gradle Groovy
- Spring Kafka (KafkaAdminClient)
- JUnit 5 + Mockito for tests (60% coverage minimum)

## REST Endpoints

### Topics
- `GET /api/topics` - List all topics
- `GET /api/topics/{name}` - Describe topic (partitions, replicas, configs)
- `POST /api/topics` - Create topic (name, partitions, replication factor)
- `DELETE /api/topics/{name}` - Delete topic

### ACLs
- `GET /api/acls` - List all ACL bindings
- `POST /api/acls` - Create ACL binding
- `DELETE /api/acls` - Delete matching ACL bindings

### Cluster
- `GET /api/cluster` - Cluster info (ID, controller, nodes)

## Project Structure

- `controller/` - TopicController, AclController, ClusterController
- `service/` - TopicService, AclService, ClusterService
- `model/` - Request/response records (CreateTopicRequest, TopicInfo, AclBindingInfo, etc.)
- `config/` - KafkaAdminConfig (mTLS AdminClient bean)
