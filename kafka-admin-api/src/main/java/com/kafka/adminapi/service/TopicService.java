package com.kafka.adminapi.service;

import com.kafka.adminapi.model.CreateTopicRequest;
import com.kafka.adminapi.model.TopicInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TopicService {

    private final AdminClient adminClient;

    public TopicService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public List<String> listTopics() {
        try {
            var topics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
            return topics.stream().sorted().toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list topics", e);
        }
    }

    public TopicInfo describeTopic(String name) {
        try {
            var descriptions = adminClient.describeTopics(Collections.singleton(name))
                    .allTopicNames()
                    .get(30, TimeUnit.SECONDS);

            var description = descriptions.get(name);
            if (description == null) {
                throw new RuntimeException("Topic not found: " + name);
            }

            int partitions = description.partitions().size();
            int replicationFactor = description.partitions().isEmpty()
                    ? 0
                    : description.partitions().get(0).replicas().size();

            return new TopicInfo(name, partitions, replicationFactor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to describe topic: " + name, e);
        }
    }

    public void createTopic(CreateTopicRequest request) {
        try {
            var newTopic = new NewTopic(request.name(), request.partitions(), (short) request.replicationFactor());
            adminClient.createTopics(Collections.singleton(newTopic))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create topic: " + request.name(), e);
        }
    }

    public void deleteTopic(String name) {
        try {
            adminClient.deleteTopics(Collections.singleton(name))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete topic: " + name, e);
        }
    }
}
