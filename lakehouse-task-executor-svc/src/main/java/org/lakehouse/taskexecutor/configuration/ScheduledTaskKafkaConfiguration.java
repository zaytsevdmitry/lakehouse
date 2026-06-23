/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
