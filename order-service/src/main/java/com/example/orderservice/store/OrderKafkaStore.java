package com.example.orderservice.store;

import com.example.orderservice.model.Order;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * In-memory materialized view of the orders topic.
 *
 * PATTERN: "Kafka as a Store"
 * ─────────────────────────────────────────────────────────
 *  1. The Kafka topic (with log compaction) is the source of truth.
 *  2. On startup, we replay the entire compacted topic to rebuild
 *     a ConcurrentHashMap (the "materialized view").
 *  3. All writes go through KafkaTemplate → topic first.
 *  4. A @KafkaListener consumes from the same topic and updates
 *     the local map, keeping every instance in sync.
 *
 *  This is functionally equivalent to a KTable in Kafka Streams,
 *  but implemented with plain Spring Kafka.
 * ─────────────────────────────────────────────────────────
 */
@Component
public class OrderKafkaStore {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaStore.class);

    private final ConcurrentHashMap<String, Order> store = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final KafkaConsumer<String, Order> bootstrapConsumer;
    private final String ordersTopic;
    private final CountDownLatch bootstrapLatch = new CountDownLatch(1);

    public OrderKafkaStore(KafkaTemplate<String, Order> kafkaTemplate,
                           KafkaConsumer<String, Order> bootstrapConsumer,
                           @Value("${app.kafka.topics.orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.bootstrapConsumer = bootstrapConsumer;
        this.ordersTopic = ordersTopic;
    }

    // ───────────────────── Bootstrap (startup replay) ─────────────────────

    /**
     * On startup, manually assign all partitions and poll from the beginning
     * until we've caught up to the end offsets. This rebuilds the full
     * in-memory state from the compacted topic.
     */
    @PostConstruct
    public void bootstrap() {
        log.info("Bootstrapping order store from topic '{}'...", ordersTopic);

        try {
            // Get partition info and assign all partitions manually
            List<TopicPartition> partitions = bootstrapConsumer
                    .partitionsFor(ordersTopic)
                    .stream()
                    .map(pi -> new TopicPartition(pi.topic(), pi.partition()))
                    .collect(Collectors.toList());

            if (partitions.isEmpty()) {
                log.info("Topic '{}' has no partitions yet. Starting with empty store.", ordersTopic);
                bootstrapLatch.countDown();
                return;
            }

            bootstrapConsumer.assign(partitions);
            bootstrapConsumer.seekToBeginning(partitions);

            // Determine the end offsets so we know when we've caught up
            Map<TopicPartition, Long> endOffsets = bootstrapConsumer.endOffsets(partitions);

            int recordCount = 0;
            boolean caughtUp = false;

            while (!caughtUp) {
                ConsumerRecords<String, Order> records = bootstrapConsumer.poll(Duration.ofSeconds(2));

                records.forEach(record -> {
                    if (record.value() == null || record.value().isDeleted()) {
                        // Tombstone or soft-deleted → remove from store
                        store.remove(record.key());
                    } else {
                        store.put(record.key(), record.value());
                    }
                });

                recordCount += records.count();

                // Check if we've reached the end offsets on every partition
                caughtUp = true;
                for (TopicPartition tp : partitions) {
                    long position = bootstrapConsumer.position(tp);
                    long endOffset = endOffsets.getOrDefault(tp, 0L);
                    if (position < endOffset) {
                        caughtUp = false;
                        break;
                    }
                }

                // If topic was empty from the start
                if (endOffsets.values().stream().allMatch(offset -> offset == 0)) {
                    caughtUp = true;
                }
            }

            log.info("Bootstrap complete. Loaded {} orders from {} records.", store.size(), recordCount);

        } catch (Exception e) {
            log.error("Failed to bootstrap order store", e);
        } finally {
            bootstrapConsumer.close();
            bootstrapLatch.countDown();
        }
    }

    // ───────────────────── Live listener (post-bootstrap) ─────────────────

    /**
     * After bootstrap, the Spring @KafkaListener keeps the local map
     * in sync with any new writes to the topic (including writes from
     * other instances of this service).
     */
    @KafkaListener(topics = "${app.kafka.topics.orders}",
                   groupId = "${app.kafka.consumer-group}")
    public void onOrderEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, Order> record) {
        log.debug("Received order event: key={}", record.key());

        if (record.value() == null || record.value().isDeleted()) {
            store.remove(record.key());
        } else {
            store.put(record.key(), record.value());
        }
    }

    // ───────────────────── Write path ─────────────────────────────────────

    /**
     * Produce the order to Kafka. The topic is the source of truth;
     * the local map will be updated asynchronously via the listener.
     * We also update the local map optimistically for immediate read-after-write.
     */
    public CompletableFuture<SendResult<String, Order>> put(Order order) {
        // Optimistic local update for immediate consistency on this instance
        if (order.isDeleted()) {
            store.remove(order.getId());
        } else {
            store.put(order.getId(), order);
        }

        return kafkaTemplate.send(ordersTopic, order.getId(), order);
    }

    // ───────────────────── Read path ──────────────────────────────────────

    public Optional<Order> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Collection<Order> getAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    public int size() {
        return store.size();
    }

    public void awaitBootstrap() throws InterruptedException {
        bootstrapLatch.await();
    }
}
