package com.kafka.synthetic.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    @Value("${kafka.ssl.keystore-location}")
    private String keystoreLocation;

    @Value("${kafka.ssl.keystore-password}")
    private String keystorePassword;

    @Value("${kafka.ssl.truststore-location}")
    private String truststoreLocation;

    @Value("${kafka.ssl.truststore-password}")
    private String truststorePassword;

    @Bean
    public KafkaProducer<String, GenericRecord> kafkaProducer() {
        Map<String, Object> props = new HashMap<>();

        // Basic config
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);

        // mTLS config
        props.put("security.protocol", "SSL");
        props.put("ssl.keystore.location", keystoreLocation);
        props.put("ssl.keystore.password", keystorePassword);
        props.put("ssl.truststore.location", truststoreLocation);
        props.put("ssl.truststore.password", truststorePassword);

        // Producer specific
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        return new KafkaProducer<>(props);
    }
}
