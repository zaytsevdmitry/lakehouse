/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.scheduler.service;

import org.jarina.client.api.constant.Types;
import org.jarina.client.api.dto.configs.ConfigurationChangeDTO;
import org.jarina.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The scheduler consumes schema changes from Kafka. The schedule object arriving in the change
 * envelope must be passed to {@link BuildService#registration(ScheduleEffectiveDTO)} with its domain
 * (present or absent) preserved, so that the domain can be persisted into the schedule tables.
 */
class ConfigurationChangeConsumerDomainTest {

    private static ConfigurationChangeDTO change(Map<String, Object> object) {
        return new ConfigurationChangeDTO(
                ConfigurationChangeConsumerService.SCHEDULE_KIND,
                "initial",
                OffsetDateTime.now(),
                object,
                Types.configAction.SAVE);
    }

    private static ScheduleEffectiveDTO registerAndCapture(ConfigurationChangeDTO change) throws Exception {
        BuildService buildService = mock(BuildService.class);
        ScheduleEffectiveService scheduleEffectiveService = mock(ScheduleEffectiveService.class);
        TaskExecutionServiceGroupConfigService taskGroupService =
                mock(TaskExecutionServiceGroupConfigService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        ConfigurationChangeConsumerService consumer = new ConfigurationChangeConsumerService(
                buildService, scheduleEffectiveService, taskGroupService);

        consumer.listen(change, acknowledgment);

        ArgumentCaptor<ScheduleEffectiveDTO> captor = ArgumentCaptor.forClass(ScheduleEffectiveDTO.class);
        verify(buildService).registration(captor.capture());
        verify(acknowledgment).acknowledge();
        return captor.getValue();
    }

    @Test
    void passesScheduleDomainFromKafkaEnvelopeToRegistration() throws Exception {
        Map<String, Object> object = new HashMap<>();
        object.put("keyName", "initial");
        object.put("domainKeyName", "analytics");
        object.put("intervalExpression", "0 0 0 * * *");
        object.put("startDateTime", "2025-01-01T00:00:00z");

        ScheduleEffectiveDTO captured = registerAndCapture(change(object));

        assertThat(captured.getKeyName()).isEqualTo("initial");
        assertThat(captured.getDomainKeyName()).isEqualTo("analytics");
    }

    @Test
    void passesNullDomainWhenScheduleHasNone() throws Exception {
        Map<String, Object> object = new HashMap<>();
        object.put("keyName", "initial");
        object.put("intervalExpression", "0 0 0 * * *");
        object.put("startDateTime", "2025-01-01T00:00:00z");

        ScheduleEffectiveDTO captured = registerAndCapture(change(object));

        assertThat(captured.getDomainKeyName()).isNull();
    }
}
