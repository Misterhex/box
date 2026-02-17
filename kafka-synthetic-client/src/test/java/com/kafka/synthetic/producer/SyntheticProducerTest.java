package com.kafka.synthetic.producer;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyntheticProducerTest {

    @Mock
    private KafkaProducer<String, GenericRecord> mockProducer;

    private SyntheticProducer syntheticProducer;

    private static final String TEST_TOPIC = "test-topic";

    @BeforeEach
    void setUp() throws Exception {
        syntheticProducer = new SyntheticProducer(mockProducer, TEST_TOPIC);
        syntheticProducer.init();
    }

    @Test
    void testProduceMessage() {
        // Arrange
        when(mockProducer.send(any(ProducerRecord.class), any()))
            .thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));

        // Act
        syntheticProducer.produceMessage();

        // Assert
        ArgumentCaptor<ProducerRecord<String, GenericRecord>> captor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockProducer, times(1)).send(captor.capture(), any());

        ProducerRecord<String, GenericRecord> capturedRecord = captor.getValue();
        assertEquals(TEST_TOPIC, capturedRecord.topic());
        assertNotNull(capturedRecord.key());
        assertNotNull(capturedRecord.value());
    }

    @Test
    void testCreateSyntheticEvent() {
        // Act
        GenericRecord record = syntheticProducer.createSyntheticEvent();

        // Assert
        assertNotNull(record);
        assertNotNull(record.get("id"));
        assertNotNull(record.get("timestamp"));
        assertEquals("synthetic-producer", record.get("source").toString());
        assertNotNull(record.get("payload"));

        assertTrue(record.get("id").toString().length() > 0);
        assertTrue((Long) record.get("timestamp") > 0);
        assertTrue(record.get("payload").toString().startsWith("sample-data-"));
    }

    @Test
    void testMessageKeyFormat() {
        // Arrange
        when(mockProducer.send(any(ProducerRecord.class), any()))
            .thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));

        // Act
        syntheticProducer.produceMessage();

        // Assert
        ArgumentCaptor<ProducerRecord<String, GenericRecord>> captor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockProducer).send(captor.capture(), any());

        String key = captor.getValue().key();
        assertNotNull(key);
        // UUID format check (simple validation)
        assertTrue(key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testMessageValueStructure() {
        // Arrange
        when(mockProducer.send(any(ProducerRecord.class), any()))
            .thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));

        // Act
        syntheticProducer.produceMessage();

        // Assert
        ArgumentCaptor<ProducerRecord<String, GenericRecord>> captor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockProducer).send(captor.capture(), any());

        GenericRecord value = captor.getValue().value();
        assertNotNull(value.get("id"));
        assertNotNull(value.get("timestamp"));
        assertNotNull(value.get("source"));
        assertNotNull(value.get("payload"));
    }

    @Test
    void testMultipleMessagesHaveUniqueKeys() {
        // Arrange
        when(mockProducer.send(any(ProducerRecord.class), any()))
            .thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));

        // Act
        syntheticProducer.produceMessage();
        syntheticProducer.produceMessage();

        // Assert
        ArgumentCaptor<ProducerRecord<String, GenericRecord>> captor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockProducer, times(2)).send(captor.capture(), any());

        var records = captor.getAllValues();
        assertNotEquals(records.get(0).key(), records.get(1).key());
    }
}
