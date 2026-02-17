package com.kafka.synthetic.schema;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

@Service
public class SchemaRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistrar.class);

    private final RestClient restClient;
    private final String schemaRegistryUrl;
    private final String topic;

    public SchemaRegistrar(
            @Value("${kafka.schema-registry.url}") String schemaRegistryUrl,
            @Value("${kafka.topic}") String topic) {
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.topic = topic;
        this.restClient = RestClient.create();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerSchema() {
        try {
            Schema schema = loadSchema("avro/synthetic-event.avsc");
            String subject = topic + "-value";

            logger.info("Registering schema for subject: {}", subject);

            restClient.post()
                .uri(schemaRegistryUrl + "/subjects/" + subject + "/versions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("schema", schema.toString()))
                .retrieve()
                .toBodilessEntity();

            logger.info("Schema registered successfully for subject: {}", subject);
        } catch (Exception e) {
            logger.error("Failed to register schema", e);
        }
    }

    Schema loadSchema(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return new Schema.Parser().parse(resource.getInputStream());
    }
}
