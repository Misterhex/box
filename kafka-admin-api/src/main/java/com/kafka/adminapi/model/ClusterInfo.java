package com.kafka.adminapi.model;

import java.util.List;

public record ClusterInfo(String clusterId, List<NodeInfo> nodes) {
}
