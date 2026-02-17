package com.kafka.adminapi.service;

import com.kafka.adminapi.model.ClusterInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private AdminClient adminClient;

    @Mock
    private DescribeClusterResult describeClusterResult;

    private ClusterService clusterService;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterService(adminClient);
    }

    @Test
    void getClusterInfo_shouldReturnClusterInfo() throws Exception {
        // Given
        String clusterId = "test-cluster-id";
        Collection<Node> nodes = Arrays.asList(
                new Node(1, "host1", 9092),
                new Node(2, "host2", 9092)
        );

        KafkaFuture<String> clusterIdFuture = KafkaFuture.completedFuture(clusterId);
        KafkaFuture<Collection<Node>> nodesFuture = KafkaFuture.completedFuture(nodes);

        when(adminClient.describeCluster()).thenReturn(describeClusterResult);
        when(describeClusterResult.clusterId()).thenReturn(clusterIdFuture);
        when(describeClusterResult.nodes()).thenReturn(nodesFuture);

        // When
        ClusterInfo result = clusterService.getClusterInfo();

        // Then
        assertNotNull(result);
        assertEquals(clusterId, result.clusterId());
        assertEquals(2, result.nodes().size());
        assertEquals(1, result.nodes().get(0).id());
        assertEquals("host1", result.nodes().get(0).host());
        assertEquals(9092, result.nodes().get(0).port());
        verify(adminClient).describeCluster();
    }

    @Test
    void getClusterInfo_shouldThrowExceptionOnFailure() throws Exception {
        // Given
        KafkaFuture<String> failedFuture = mock(KafkaFuture.class);
        when(failedFuture.get(anyLong(), any())).thenThrow(new RuntimeException("Connection failed"));
        when(adminClient.describeCluster()).thenReturn(describeClusterResult);
        when(describeClusterResult.clusterId()).thenReturn(failedFuture);

        // When / Then
        assertThrows(RuntimeException.class, () -> clusterService.getClusterInfo());
        verify(adminClient).describeCluster();
    }
}
