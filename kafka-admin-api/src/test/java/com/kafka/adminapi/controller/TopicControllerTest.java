package com.kafka.adminapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.adminapi.model.CreateTopicRequest;
import com.kafka.adminapi.model.TopicInfo;
import com.kafka.adminapi.service.TopicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TopicController.class)
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    @Test
    void listTopics_shouldReturnTopicList() throws Exception {
        // Given
        List<String> topics = Arrays.asList("topic-a", "topic-b", "topic-c");
        when(topicService.listTopics()).thenReturn(topics);

        // When / Then
        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("topic-a"))
                .andExpect(jsonPath("$[1]").value("topic-b"))
                .andExpect(jsonPath("$[2]").value("topic-c"));

        verify(topicService).listTopics();
    }

    @Test
    void getTopic_shouldReturnTopicInfo() throws Exception {
        // Given
        TopicInfo topicInfo = new TopicInfo("test-topic", 3, 2);
        when(topicService.describeTopic("test-topic")).thenReturn(topicInfo);

        // When / Then
        mockMvc.perform(get("/api/topics/test-topic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-topic"))
                .andExpect(jsonPath("$.partitions").value(3))
                .andExpect(jsonPath("$.replicationFactor").value(2));

        verify(topicService).describeTopic("test-topic");
    }

    @Test
    void createTopic_shouldCreateTopicSuccessfully() throws Exception {
        // Given
        CreateTopicRequest request = new CreateTopicRequest("new-topic", 3, 2);
        doNothing().when(topicService).createTopic(any(CreateTopicRequest.class));

        // When / Then
        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(topicService).createTopic(any(CreateTopicRequest.class));
    }

    @Test
    void deleteTopic_shouldDeleteTopicSuccessfully() throws Exception {
        // Given
        doNothing().when(topicService).deleteTopic("topic-to-delete");

        // When / Then
        mockMvc.perform(delete("/api/topics/topic-to-delete"))
                .andExpect(status().isNoContent());

        verify(topicService).deleteTopic("topic-to-delete");
    }

    @Test
    void getTopic_shouldReturnErrorOnServiceFailure() throws Exception {
        // Given
        when(topicService.describeTopic("non-existent-topic"))
                .thenThrow(new RuntimeException("Topic not found"));

        // When / Then
        mockMvc.perform(get("/api/topics/non-existent-topic"))
                .andExpect(status().is5xxServerError());

        verify(topicService).describeTopic("non-existent-topic");
    }

    @Test
    void createTopic_shouldReturnErrorOnInvalidRequest() throws Exception {
        // Given - invalid JSON
        String invalidJson = "{\"name\": \"test\"}";

        // When / Then
        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is4xxClientError());

        verify(topicService, never()).createTopic(any());
    }
}
