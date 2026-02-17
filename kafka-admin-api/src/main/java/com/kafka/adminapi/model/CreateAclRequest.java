package com.kafka.adminapi.model;

public record CreateAclRequest(
        String principal,
        String resourceType,
        String resourceName,
        String operation,
        String permission
) {
}
