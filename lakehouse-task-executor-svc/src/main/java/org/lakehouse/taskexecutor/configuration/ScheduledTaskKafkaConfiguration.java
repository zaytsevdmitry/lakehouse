/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
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

    @Autowired
    ScheduledTaskKafkaConfigurationProperties scheduledTaskKafkaConfigurationProperties;


    private Map<String, Object> getConsumerProperties() {
        Map<String, Object> props = new HashMap<>(scheduledTaskKafkaConfigurationProperties.getProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ScheduledTaskMsgKafkaDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.forEach((key, o) -> logger.info("ScheduledTask consumer properties {} -> {}", key, o.toString()));
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, ScheduledTaskMsgDTO> containerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, ScheduledTaskMsgDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<Long, ScheduledTaskMsgDTO>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(getConsumerProperties()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setSyncCommits(true);

        logger.info("consumer factory created");
        return factory;
    }

}
