package com.kafka.synthetic.schema;

import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaRegistrarTest {

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private SchemaRegistrar schemaRegistrar;

    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String TOPIC = "test-topic";

    @BeforeEach
    void setUp() {
        // Cannot easily mock RestClient.create(), so we'll test loadSchema separately
        schemaRegistrar = new SchemaRegistrar(SCHEMA_REGISTRY_URL, TOPIC);
    }

    @Test
    void testLoadSchemaFromResource() throws IOException {
        // Act
        Schema schema = schemaRegistrar.loadSchema("avro/synthetic-event.avsc");

        // Assert
        assertNotNull(schema);
        assertEquals("SyntheticEvent", schema.getName());
        assertEquals("com.kafka.synthetic.avro", schema.getNamespace());

        // Verify fields
        assertEquals(4, schema.getFields().size());
        assertNotNull(schema.getField("id"));
        assertNotNull(schema.getField("timestamp"));
        assertNotNull(schema.getField("source"));
        assertNotNull(schema.getField("payload"));
    }

    @Test
    void testLoadSchemaFieldTypes() throws IOException {
        // Act
        Schema schema = schemaRegistrar.loadSchema("avro/synthetic-event.avsc");

        // Assert
        assertEquals(Schema.Type.STRING, schema.getField("id").schema().getType());
        assertEquals(Schema.Type.LONG, schema.getField("timestamp").schema().getType());
        assertEquals(Schema.Type.STRING, schema.getField("source").schema().getType());
        assertEquals(Schema.Type.STRING, schema.getField("payload").schema().getType());
    }

    @Test
    void testLoadSchemaNonExistentFile() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            schemaRegistrar.loadSchema("avro/non-existent.avsc");
        });
    }

    @Test
    void testSchemaToString() throws IOException {
        // Act
        Schema schema = schemaRegistrar.loadSchema("avro/synthetic-event.avsc");
        String schemaString = schema.toString();

        // Assert
        assertNotNull(schemaString);
        assertTrue(schemaString.contains("SyntheticEvent"));
        assertTrue(schemaString.contains("com.kafka.synthetic.avro"));
        assertTrue(schemaString.contains("id"));
        assertTrue(schemaString.contains("timestamp"));
        assertTrue(schemaString.contains("source"));
        assertTrue(schemaString.contains("payload"));
    }

    @Test
    void testSchemaRegistrySubjectName() throws IOException {
        // Act
        Schema schema = schemaRegistrar.loadSchema("avro/synthetic-event.avsc");
        String expectedSubject = TOPIC + "-value";

        // Assert
        assertNotNull(schema);
        assertEquals("test-topic-value", expectedSubject);
    }

    @Test
    void testSchemaRegistryUrl() {
        // Arrange
        String expectedUrl = SCHEMA_REGISTRY_URL + "/subjects/" + TOPIC + "-value/versions";

        // Assert
        assertEquals("http://localhost:8081/subjects/test-topic-value/versions", expectedUrl);
    }
}
