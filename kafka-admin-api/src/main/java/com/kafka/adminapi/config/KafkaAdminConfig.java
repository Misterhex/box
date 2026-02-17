package com.kafka.adminapi.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaAdminConfig {

    @Bean
    public AdminClient adminClient(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.ssl.keystore-location}") String keystoreLocation,
            @Value("${kafka.ssl.keystore-password}") String keystorePassword,
            @Value("${kafka.ssl.truststore-location}") String truststoreLocation,
            @Value("${kafka.ssl.truststore-password}") String truststorePassword
    ) {
        var props = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL",
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation,
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword,
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword
        );
        return AdminClient.create(props);
    }
}
