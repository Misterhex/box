package com.kafka.synthetic.consumer;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SyntheticConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SyntheticConsumer.class);

    private final KafkaConsumer<String, GenericRecord> consumer;
    private final String topic;
    private final ExecutorService executor;
    private final AtomicBoolean running;

    public SyntheticConsumer(
            KafkaConsumer<String, GenericRecord> consumer,
            @Value("${kafka.topic}") String topic) {
        this.consumer = consumer;
        this.topic = topic;
        this.executor = Executors.newSingleThreadExecutor();
        this.running = new AtomicBoolean(false);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        if (running.compareAndSet(false, true)) {
            consumer.subscribe(List.of(topic));
            logger.info("SyntheticConsumer started for topic: {}", topic);

            executor.submit(() -> {
                while (running.get()) {
                    try {
                        ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(1000));
                        for (ConsumerRecord<String, GenericRecord> record : records) {
                            processRecord(record);
                        }
                    } catch (Exception e) {
                        logger.error("Error while consuming messages", e);
                    }
                }
            });
        }
    }

    void processRecord(ConsumerRecord<String, GenericRecord> record) {
        GenericRecord value = record.value();
        boolean isValid = validateRecord(value);

        if (isValid) {
            logger.info("Consumed valid message - Key: {}, Partition: {}, Offset: {}, Value: {}",
                record.key(), record.partition(), record.offset(), value);
        } else {
            logger.warn("Consumed invalid message - Key: {}, Partition: {}, Offset: {}, Value: {}",
                record.key(), record.partition(), record.offset(), value);
        }
    }

    boolean validateRecord(GenericRecord record) {
        if (record == null) {
            return false;
        }

        try {
            Object id = record.get("id");
            Object timestamp = record.get("timestamp");
            Object source = record.get("source");
            Object payload = record.get("payload");

            boolean hasAllFields = id != null && timestamp != null && source != null && payload != null;
            boolean isValidId = id instanceof CharSequence && !id.toString().isEmpty();
            boolean isValidTimestamp = timestamp instanceof Long && (Long) timestamp > 0;
            boolean isValidSource = source instanceof CharSequence && !source.toString().isEmpty();
            boolean isValidPayload = payload instanceof CharSequence && !payload.toString().isEmpty();

            return hasAllFields && isValidId && isValidTimestamp && isValidSource && isValidPayload;
        } catch (Exception e) {
            logger.error("Error validating record", e);
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        executor.shutdown();
        consumer.close();
        logger.info("SyntheticConsumer stopped");
    }
}
