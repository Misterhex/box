package com.kafka.adminapi.controller;

import com.kafka.adminapi.model.ClusterInfo;
import com.kafka.adminapi.model.NodeInfo;
import com.kafka.adminapi.service.ClusterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClusterController.class)
class ClusterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClusterService clusterService;

    @Test
    void getClusterInfo_shouldReturnClusterInfo() throws Exception {
        // Given
        ClusterInfo clusterInfo = new ClusterInfo(
                "test-cluster",
                Arrays.asList(
                        new NodeInfo(1, "host1", 9092),
                        new NodeInfo(2, "host2", 9092)
                )
        );
        when(clusterService.getClusterInfo()).thenReturn(clusterInfo);

        // When / Then
        mockMvc.perform(get("/api/cluster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusterId").value("test-cluster"))
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].id").value(1))
                .andExpect(jsonPath("$.nodes[0].host").value("host1"))
                .andExpect(jsonPath("$.nodes[0].port").value(9092))
                .andExpect(jsonPath("$.nodes[1].id").value(2));

        verify(clusterService).getClusterInfo();
    }

    @Test
    void getClusterInfo_shouldReturnErrorOnServiceFailure() throws Exception {
        // Given
        when(clusterService.getClusterInfo()).thenThrow(new RuntimeException("Connection failed"));

        // When / Then
        mockMvc.perform(get("/api/cluster"))
                .andExpect(status().is5xxServerError());

        verify(clusterService).getClusterInfo();
    }
}
