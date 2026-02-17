package com.kafka.adminapi.service;

import com.kafka.adminapi.model.ClusterInfo;
import com.kafka.adminapi.model.NodeInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ClusterService {

    private final AdminClient adminClient;

    public ClusterService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public ClusterInfo getClusterInfo() {
        try {
            var describeCluster = adminClient.describeCluster();
            var clusterId = describeCluster.clusterId().get(30, TimeUnit.SECONDS);
            var nodes = describeCluster.nodes().get(30, TimeUnit.SECONDS);

            var nodeInfoList = nodes.stream()
                    .map(node -> new NodeInfo(node.id(), node.host(), node.port()))
                    .toList();

            return new ClusterInfo(clusterId, nodeInfoList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cluster info", e);
        }
    }
}
