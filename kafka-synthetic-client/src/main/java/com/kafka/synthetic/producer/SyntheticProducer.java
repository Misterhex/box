package com.kafka.synthetic.producer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SyntheticProducer {

    private static final Logger logger = LoggerFactory.getLogger(SyntheticProducer.class);

    private final KafkaProducer<String, GenericRecord> producer;
    private final String topic;
    private Schema schema;

    public SyntheticProducer(
            KafkaProducer<String, GenericRecord> producer,
            @Value("${kafka.topic}") String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("avro/synthetic-event.avsc");
        this.schema = new Schema.Parser().parse(resource.getInputStream());
        logger.info("SyntheticProducer initialized for topic: {}", topic);
    }

    @Scheduled(fixedRateString = "${kafka.producer.rate-per-second:1}000")
    public void produceMessage() {
        String key = UUID.randomUUID().toString();
        GenericRecord record = createSyntheticEvent();

        ProducerRecord<String, GenericRecord> producerRecord = new ProducerRecord<>(topic, key, record);

        producer.send(producerRecord, (metadata, exception) -> {
            if (exception != null) {
                logger.error("Failed to send message with key: {}", key, exception);
            } else {
                logger.info("Produced message - Key: {}, Partition: {}, Offset: {}, Value: {}",
                    key, metadata.partition(), metadata.offset(), record);
            }
        });
    }

    GenericRecord createSyntheticEvent() {
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", UUID.randomUUID().toString());
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "synthetic-producer");
        record.put("payload", generateRandomPayload());
        return record;
    }

    private String generateRandomPayload() {
        String[] samples = {
            "sample-data-alpha",
            "sample-data-beta",
            "sample-data-gamma",
            "sample-data-delta",
            "sample-data-epsilon"
        };
        int index = ThreadLocalRandom.current().nextInt(samples.length);
        return samples[index] + "-" + System.currentTimeMillis();
    }
}
