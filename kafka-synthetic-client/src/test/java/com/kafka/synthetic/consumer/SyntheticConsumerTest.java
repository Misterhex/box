package com.kafka.synthetic.consumer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyntheticConsumerTest {

    @Mock
    private KafkaConsumer<String, GenericRecord> mockConsumer;

    private SyntheticConsumer syntheticConsumer;

    private static final String TEST_TOPIC = "test-topic";
    private Schema schema;

    @BeforeEach
    void setUp() {
        syntheticConsumer = new SyntheticConsumer(mockConsumer, TEST_TOPIC);

        String schemaJson = """
            {
              "type": "record",
              "name": "SyntheticEvent",
              "namespace": "com.kafka.synthetic.avro",
              "fields": [
                {"name": "id", "type": "string"},
                {"name": "timestamp", "type": "long"},
                {"name": "source", "type": "string"},
                {"name": "payload", "type": "string"}
              ]
            }
            """;
        schema = new Schema.Parser().parse(schemaJson);
    }

    @Test
    void testValidateRecordWithValidData() {
        // Arrange
        GenericRecord record = createValidRecord();

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateRecordWithNullRecord() {
        // Act
        boolean result = syntheticConsumer.validateRecord(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRecordWithMissingId() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", null);
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "test-source");
        record.put("payload", "test-payload");

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRecordWithEmptyId() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "");
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "test-source");
        record.put("payload", "test-payload");

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRecordWithInvalidTimestamp() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "test-id");
        record.put("timestamp", 0L);
        record.put("source", "test-source");
        record.put("payload", "test-payload");

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRecordWithEmptySource() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "test-id");
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "");
        record.put("payload", "test-payload");

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRecordWithEmptyPayload() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "test-id");
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "test-source");
        record.put("payload", "");

        // Act
        boolean result = syntheticConsumer.validateRecord(record);

        // Assert
        assertFalse(result);
    }

    @Test
    void testProcessRecordWithValidRecord() {
        // Arrange
        GenericRecord record = createValidRecord();
        ConsumerRecord<String, GenericRecord> consumerRecord =
            new ConsumerRecord<>(TEST_TOPIC, 0, 0L, "test-key", record);

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> syntheticConsumer.processRecord(consumerRecord));
    }

    @Test
    void testProcessRecordWithInvalidRecord() {
        // Arrange
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "");
        record.put("timestamp", 0L);
        record.put("source", "");
        record.put("payload", "");

        ConsumerRecord<String, GenericRecord> consumerRecord =
            new ConsumerRecord<>(TEST_TOPIC, 0, 0L, "test-key", record);

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> syntheticConsumer.processRecord(consumerRecord));
    }

    @Test
    void testStartConsumingSubscribesToTopic() {
        // Arrange
        when(mockConsumer.poll(any(Duration.class)))
            .thenReturn(new ConsumerRecords<>(Map.of()));

        // Act
        syntheticConsumer.startConsuming();

        // Give some time for the executor to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        verify(mockConsumer, times(1)).subscribe(List.of(TEST_TOPIC));
        verify(mockConsumer, atLeastOnce()).poll(any(Duration.class));

        // Cleanup
        syntheticConsumer.shutdown();
    }

    private GenericRecord createValidRecord() {
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "test-id-123");
        record.put("timestamp", System.currentTimeMillis());
        record.put("source", "test-source");
        record.put("payload", "test-payload-data");
        return record;
    }
}
