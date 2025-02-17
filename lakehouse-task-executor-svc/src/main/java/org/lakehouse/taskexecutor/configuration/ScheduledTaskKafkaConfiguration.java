package org.lakehouse.taskexecutor.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaDeserializer;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduledTaskKafkaConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
/*
    @Value("${lakehouse.executor.scheduled.task.kafka.consumer.bootstrap-servers}" )
    private String bootstrapServers;
    @Value("${lakehouse.scheduler.config.schedule.kafka.consumer.group-id}" )
    private String consumerGroup;
    @Value("${lakehouse.scheduler.config.schedule.kafka.consumer.auto-offset-reset}" )
    private String autoOffsetReset;
*/

    @Autowired
    ScheduledTaskKafkaConfigurationProperties scheduledTaskKafkaConfigurationProperties;


    private Map<String,Object> getConsumerProperties(){
        Map<String,Object> props = new HashMap<>(scheduledTaskKafkaConfigurationProperties.getProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ScheduledTaskMsgKafkaDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.forEach((key, o) -> logger.info("ScheduledTask consumer properties {} -> {}", key, o.toString()));
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, ScheduledTaskMsgDTO> containerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long,  ScheduledTaskMsgDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<Long, ScheduledTaskMsgDTO>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(getConsumerProperties()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setSyncCommits(true);

        logger.info("consumer factory created");
        return factory;
    }

}
