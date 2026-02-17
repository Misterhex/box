package com.kafka.adminapi.controller;

import com.kafka.adminapi.model.ClusterInfo;
import com.kafka.adminapi.service.ClusterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterService clusterService;

    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @GetMapping
    public ClusterInfo getClusterInfo() {
        return clusterService.getClusterInfo();
    }
}
