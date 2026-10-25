/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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
package org.yarina.config.component;

import org.yarina.client.api.constant.Types;
import org.yarina.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.yarina.config.entities.ConfigurationProduceMessage;
import org.yarina.config.produce.ConfigurationProduceResolver;
import org.yarina.config.produce.ConfigurationProduceResolverRegistry;
import org.yarina.config.repository.ConfigurationProduceMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The schedule configuration published to Kafka must carry the domain of the schedule,
 * because the scheduler service persists it into {@code schedule_instance.domain_key_name}.
 * A schedule without a domain must be published as {@code domainKeyName: null}.
 */
class ConfigurationProduceSchedulerDomainTest {

    private static final String TOPIC = "configuration_changes";
    private static final String KIND = "Schedule";
    private static final String KEY_NAME = "initial";

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, String> sendOneAndCapture(ConfigurationProduceMessage message,
                                                                   ScheduleEffectiveDTO resolved) {
        ConfigurationProduceMessageRepository repository = mock(ConfigurationProduceMessageRepository.class);
        ConfigurationProduceResolverRegistry registry = mock(ConfigurationProduceResolverRegistry.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        when(repository.findAllWithLimit(any())).thenReturn(List.of(message));
        ConfigurationProduceResolver resolver = mock(ConfigurationProduceResolver.class);
        when(resolver.resolve(KEY_NAME)).thenReturn(resolved);
        when(registry.findByKind(KIND)).thenReturn(Optional.of(resolver));

        new ConfigurationProduceScheduler(repository, registry, kafkaTemplate, TOPIC, 100).sendChanges();

        return kafkaTemplate;
    }

    @Test
    void sendsDomainOfTheScheduleToKafka() {
        ScheduleEffectiveDTO resolved = new ScheduleEffectiveDTO();
        resolved.setKeyName(KEY_NAME);
        resolved.setDomainKeyName("analytics");

        KafkaTemplate<String, String> kafkaTemplate = sendOneAndCapture(
                new ConfigurationProduceMessage(KIND, KEY_NAME, OffsetDateTime.now(), Types.configAction.SAVE),
                resolved);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(KEY_NAME), payload.capture());

        assertThat(payload.getValue())
                .contains("\"kind\":\"Schedule\"")
                .contains("\"keyName\":\"initial\"")
                .contains("\"domainKeyName\":\"analytics\"");
    }

    @Test
    void sendsNullDomainWhenTheScheduleHasNone() {
        ScheduleEffectiveDTO resolved = new ScheduleEffectiveDTO();
        resolved.setKeyName(KEY_NAME);
        resolved.setDomainKeyName(null);

        KafkaTemplate<String, String> kafkaTemplate = sendOneAndCapture(
                new ConfigurationProduceMessage(KIND, KEY_NAME, OffsetDateTime.now(), Types.configAction.SAVE),
                resolved);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(KEY_NAME), payload.capture());

        assertThat(payload.getValue()).contains("\"domainKeyName\":null");
    }
}
