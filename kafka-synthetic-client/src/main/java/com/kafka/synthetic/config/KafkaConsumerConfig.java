package com.kafka.synthetic.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    @Value("${kafka.group-id}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${kafka.ssl.keystore-location}")
    private String keystoreLocation;

    @Value("${kafka.ssl.keystore-password}")
    private String keystorePassword;

    @Value("${kafka.ssl.truststore-location}")
    private String truststoreLocation;

    @Value("${kafka.ssl.truststore-password}")
    private String truststorePassword;

    @Bean
    public KafkaConsumer<String, GenericRecord> kafkaConsumer() {
        Map<String, Object> props = new HashMap<>();

        // Basic config
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", "false");

        // mTLS config
        props.put("security.protocol", "SSL");
        props.put("ssl.keystore.location", keystoreLocation);
        props.put("ssl.keystore.password", keystorePassword);
        props.put("ssl.truststore.location", truststoreLocation);
        props.put("ssl.truststore.password", truststorePassword);

        // Consumer specific
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        return new KafkaConsumer<>(props);
    }
}
