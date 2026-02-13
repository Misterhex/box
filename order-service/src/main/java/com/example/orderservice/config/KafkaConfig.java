package com.example.orderservice.config;

import com.example.orderservice.model.Order;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.orders}")
    private String ordersTopic;

    /**
     * Create the orders topic with log compaction enabled.
     *
     * Log compaction is the key enabler: Kafka will keep the LATEST value
     * for each key (order ID) indefinitely, while removing older duplicates.
     * This turns the topic into a key-value store that can be fully replayed
     * on startup.
     */
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(3)
                .replicas(1)
                .config("cleanup.policy", "compact")
                .config("min.cleanable.dirty.ratio", "0.1")
                .config("segment.ms", "100")
                .build();
    }

    /**
     * A dedicated KafkaConsumer used ONLY during bootstrap to replay the
     * compacted topic from the beginning. This is separate from the
     * Spring @KafkaListener consumer so we can fully control the poll loop
     * and block startup until the local store is populated.
     */
    @Bean
    public KafkaConsumer<String, Order> bootstrapConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Use a unique group ID so it always reads from the beginning
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-bootstrap-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.orderservice.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Order.class.getName());

        return new KafkaConsumer<>(props);
    }
}
