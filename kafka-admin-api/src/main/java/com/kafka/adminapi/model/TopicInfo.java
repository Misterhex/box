package com.kafka.adminapi.model;

public record TopicInfo(String name, int partitions, int replicationFactor) {
}
