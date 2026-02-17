package com.kafka.adminapi.model;

public record AclBindingInfo(
        String principal,
        String resourceType,
        String resourceName,
        String operation,
        String permission
) {
}
