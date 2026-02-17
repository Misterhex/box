package com.kafka.adminapi.controller;

import com.kafka.adminapi.model.CreateTopicRequest;
import com.kafka.adminapi.model.TopicInfo;
import com.kafka.adminapi.service.TopicService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<String> listTopics() {
        return topicService.listTopics();
    }

    @GetMapping("/{name}")
    public TopicInfo getTopic(@PathVariable String name) {
        return topicService.describeTopic(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createTopic(@RequestBody CreateTopicRequest request) {
        topicService.createTopic(request);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable String name) {
        topicService.deleteTopic(name);
    }
}
