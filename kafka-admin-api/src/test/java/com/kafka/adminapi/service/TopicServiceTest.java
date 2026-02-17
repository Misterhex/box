package com.kafka.adminapi.service;

import com.kafka.adminapi.model.CreateTopicRequest;
import com.kafka.adminapi.model.TopicInfo;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private AdminClient adminClient;

    @Mock
    private ListTopicsResult listTopicsResult;

    @Mock
    private DescribeTopicsResult describeTopicsResult;

    @Mock
    private CreateTopicsResult createTopicsResult;

    @Mock
    private DeleteTopicsResult deleteTopicsResult;

    private TopicService topicService;

    @BeforeEach
    void setUp() {
        topicService = new TopicService(adminClient);
    }

    @Test
    void listTopics_shouldReturnSortedTopicList() throws Exception {
        // Given
        Set<String> topics = new HashSet<>(Arrays.asList("topic-b", "topic-a", "topic-c"));
        KafkaFuture<Set<String>> topicsFuture = KafkaFuture.completedFuture(topics);

        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(topicsFuture);

        // When
        List<String> result = topicService.listTopics();

        // Then
        assertEquals(3, result.size());
        assertEquals("topic-a", result.get(0));
        assertEquals("topic-b", result.get(1));
        assertEquals("topic-c", result.get(2));
        verify(adminClient).listTopics();
    }

    @Test
    void describeTopic_shouldReturnTopicInfo() throws Exception {
        // Given
        String topicName = "test-topic";
        Node leader = new Node(1, "host1", 9092);
        List<Node> replicas = Arrays.asList(leader, new Node(2, "host2", 9092), new Node(3, "host3", 9092));
        TopicPartitionInfo partitionInfo = new TopicPartitionInfo(0, leader, replicas, Collections.emptyList());

        TopicDescription topicDescription = new TopicDescription(
                topicName,
                false,
                Arrays.asList(partitionInfo, partitionInfo)
        );

        Map<String, TopicDescription> descriptionsMap = Map.of(topicName, topicDescription);
        KafkaFuture<Map<String, TopicDescription>> descriptionsFuture = KafkaFuture.completedFuture(descriptionsMap);

        when(adminClient.describeTopics(any(Collection.class))).thenReturn(describeTopicsResult);
        when(describeTopicsResult.allTopicNames()).thenReturn(descriptionsFuture);

        // When
        TopicInfo result = topicService.describeTopic(topicName);

        // Then
        assertNotNull(result);
        assertEquals(topicName, result.name());
        assertEquals(2, result.partitions());
        assertEquals(3, result.replicationFactor());
        verify(adminClient).describeTopics(Collections.singleton(topicName));
    }

    @Test
    void createTopic_shouldCreateTopicSuccessfully() throws Exception {
        // Given
        CreateTopicRequest request = new CreateTopicRequest("new-topic", 3, 2);
        KafkaFuture<Void> createFuture = KafkaFuture.completedFuture(null);

        when(adminClient.createTopics(any(Collection.class))).thenReturn(createTopicsResult);
        when(createTopicsResult.all()).thenReturn(createFuture);

        // When
        topicService.createTopic(request);

        // Then
        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(adminClient).createTopics(captor.capture());
        Collection<NewTopic> capturedTopics = captor.getValue();
        assertEquals(1, capturedTopics.size());
        NewTopic newTopic = capturedTopics.iterator().next();
        assertEquals("new-topic", newTopic.name());
        assertEquals(3, newTopic.numPartitions());
        assertEquals(2, newTopic.replicationFactor());
    }

    @Test
    void deleteTopic_shouldDeleteTopicSuccessfully() throws Exception {
        // Given
        String topicName = "topic-to-delete";
        KafkaFuture<Void> deleteFuture = KafkaFuture.completedFuture(null);

        when(adminClient.deleteTopics(any(Collection.class))).thenReturn(deleteTopicsResult);
        when(deleteTopicsResult.all()).thenReturn(deleteFuture);

        // When
        topicService.deleteTopic(topicName);

        // Then
        verify(adminClient).deleteTopics(Collections.singleton(topicName));
        verify(deleteTopicsResult).all();
    }

    @Test
    void listTopics_shouldThrowExceptionOnFailure() throws Exception {
        // Given
        KafkaFuture<Set<String>> failedFuture = mock(KafkaFuture.class);
        when(failedFuture.get(anyLong(), any())).thenThrow(new RuntimeException("Connection failed"));
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(failedFuture);

        // When / Then
        assertThrows(RuntimeException.class, () -> topicService.listTopics());
    }
}
