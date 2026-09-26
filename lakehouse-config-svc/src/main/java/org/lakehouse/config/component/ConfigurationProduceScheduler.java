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

package org.lakehouse.config.component;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.dto.configs.ConfigurationChangeDTO;
import org.lakehouse.config.entities.ConfigurationProduceMessage;
import org.lakehouse.config.repository.ConfigurationProduceMessageRepository;
import org.lakehouse.config.produce.ConfigurationProduceResolver;
import org.lakehouse.config.produce.ConfigurationProduceResolverRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigurationProduceScheduler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConfigurationProduceMessageRepository configurationProduceMessageRepository;
    private final ConfigurationProduceResolverRegistry resolverRegistry;
    private final KafkaTemplate<String, String> configurationProduceKafkaTemplate;
    private final String produceTopic;
    private final Integer sendLimit;

    public ConfigurationProduceScheduler(
            ConfigurationProduceMessageRepository configurationProduceMessageRepository,
            ConfigurationProduceResolverRegistry resolverRegistry,
            KafkaTemplate<String, String> configurationProduceKafkaTemplate,
            @Value("${lakehouse.config.produce.topic}") String produceTopic,
            @Value("${lakehouse.config.produce.limit}") Integer sendLimit) {
        this.configurationProduceMessageRepository = configurationProduceMessageRepository;
        this.resolverRegistry = resolverRegistry;
        this.configurationProduceKafkaTemplate = configurationProduceKafkaTemplate;
        this.produceTopic = produceTopic;
        this.sendLimit = sendLimit;
        logger.debug("sendLimit {}", sendLimit);
    }

    /**
     * Selects the oldest recorded configuration changes in insertion order,
     * sends them to Kafka as JSON and deletes them (Transactional Outbox pattern).
     */
    @Scheduled(
            fixedDelayString = "${lakehouse.config.produce.delay-ms}",
            initialDelayString = "${lakehouse.config.produce.initial-delay-ms}")
    public void sendChanges() {
        List<ConfigurationProduceMessage> messages =
                configurationProduceMessageRepository.findAllWithLimit(Limit.of(sendLimit));
        if (messages.isEmpty()) {
            logger.debug("No configuration changes to send");
        } else {
            logger.info("Found {} configuration change(s) to send", messages.size());
        }
        for (ConfigurationProduceMessage message : messages) {
            try {
                Object object = null;
                if (message.getAction() == Types.configAction.SAVE) {
                    ConfigurationProduceResolver resolver = resolverRegistry.findByKind(message.getKind())
                            .orElseThrow(() -> new IllegalStateException(
                                    String.format("No resolver for configuration kind %s", message.getKind())));
                    object = resolver.resolve(message.getKeyName());
                }
                ConfigurationChangeDTO envelope = new ConfigurationChangeDTO(
                        message.getKind(), message.getKeyName(), message.getCreatedDateTime(), object, message.getAction());
                configurationProduceKafkaTemplate.send(produceTopic, message.getKeyName(), ObjectMapping.asJsonString(envelope));
                configurationProduceMessageRepository.delete(message);
                logger.info("Configuration change {}/{} ({}) sent", message.getKind(), message.getKeyName(), message.getAction());
            } catch (Exception e) {
                logger.error("Cannot send configuration change {}/{}: {}",
                        message.getKind(), message.getKeyName(), e.getMessage(), e);
            }
        }
    }
}