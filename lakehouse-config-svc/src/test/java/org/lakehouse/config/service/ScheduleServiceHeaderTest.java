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

package org.lakehouse.config.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.lakehouse.client.api.utils.DtoMergeUtils;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.repository.ScenarioActEdgeRepository;
import org.lakehouse.config.repository.ScenarioActRepository;
import org.lakehouse.config.repository.ScenarioActTaskEdgeRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.lakehouse.config.repository.TaskRepository;
import org.lakehouse.config.repository.dataset.DataSetRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleServiceHeaderTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);

    private final ScheduleService scheduleService = new ScheduleService(
            scheduleRepository,
            mock(DataSetRepository.class),
            mock(org.lakehouse.config.repository.ScenarioActTemplateRepository.class),
            mock(ScenarioActRepository.class),
            mock(ScenarioActEdgeRepository.class),
            mock(TaskRepository.class),
            mock(ScenarioActTaskEdgeRepository.class),
            mock(ScenarioActTemplateService.class),
            mock(ScheduleConfigProducerService.class),
            mock(DtoMergeUtils.class),
            mock(TaskService.class));

    @Test
    void findAllHeadersMapsScheduleEntitiesToHeaders() {
        Schedule schedule = new Schedule();
        schedule.setKeyName("daily");
        schedule.setDescription("Daily schedule");
        schedule.setIntervalExpression("0 0 0 * * *");
        schedule.setStartDateTime(OffsetDateTime.parse("2024-01-01T00:00:00+00:00"));
        schedule.setEndDateTime(OffsetDateTime.parse("2025-01-01T00:00:00+00:00"));
        schedule.setEnabled(true);

        when(scheduleRepository.findAll()).thenReturn(List.of(schedule));

        List<ScheduleHeaderDTO> result = scheduleService.findAllHeaders();

        assertThat(result).hasSize(1);
        ScheduleHeaderDTO header = result.get(0);
        assertThat(header.getKeyName()).isEqualTo("daily");
        assertThat(header.getDescription()).isEqualTo("Daily schedule");
        assertThat(header.getIntervalExpression()).isEqualTo("0 0 0 * * *");
        assertThat(header.getStartDateTime()).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(header.getStopDateTime()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(header.isEnabled()).isTrue();
    }

    @Test
    void findAllHeadersReturnsEmptyListWhenNoSchedules() {
        when(scheduleRepository.findAll()).thenReturn(List.of());

        List<ScheduleHeaderDTO> result = scheduleService.findAllHeaders();

        assertThat(result).isEmpty();
    }
}
