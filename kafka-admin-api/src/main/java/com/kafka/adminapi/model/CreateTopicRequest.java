package com.kafka.adminapi.model;

public record CreateTopicRequest(String name, int partitions, int replicationFactor) {
}
