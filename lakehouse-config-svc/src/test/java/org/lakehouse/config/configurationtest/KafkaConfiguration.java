package org.lakehouse.config.configurationtest;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class KafkaConfiguration {
    @Bean
    public KafkaAdmin admin(@Value("${lakehouse.config.schedule.kafka.producer.bootstrap-servers}") String servers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        return new KafkaAdmin(configs);
    }
}
