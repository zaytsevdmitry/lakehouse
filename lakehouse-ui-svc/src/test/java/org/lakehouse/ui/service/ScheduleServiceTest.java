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
package org.lakehouse.ui.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.ui.dto.ScheduleRequestDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    @Test
    void getSchedulesLoopsSelectedNamesAndAggregatesResults() {
        SchedulerRestClientApi api = mock(SchedulerRestClientApi.class);
        ScheduleService service = new ScheduleService(api, mock(ConfigRestClientApi.class));

        ScheduleRequestDTO request = new ScheduleRequestDTO();
        request.setFromDate("2024-01-01T00:00:00+00:00");
        request.setToDate("2024-12-31T23:59:59+00:00");
        request.setNames(List.of("daily", "hourly"));

        ScheduleInstanceDTO daily = instance(1L, "daily");
        ScheduleInstanceDTO hourly = instance(2L, "hourly");
        when(api.getAllByInterval(eq("daily"), any())).thenReturn(List.of(daily));
        when(api.getAllByInterval(eq("hourly"), any())).thenReturn(List.of(hourly));

        List<ScheduleInstanceDTO> result = service.getSchedules(request);

        assertThat(result).containsExactly(daily, hourly);
        verify(api).getAllByInterval(eq("daily"), any());
        verify(api).getAllByInterval(eq("hourly"), any());
    }

    @Test
    void getSchedulesFetchesAllWhenNoNamesSelected() {
        SchedulerRestClientApi api = mock(SchedulerRestClientApi.class);
        ScheduleService service = new ScheduleService(api, mock(ConfigRestClientApi.class));

        ScheduleRequestDTO request = new ScheduleRequestDTO();
        request.setFromDate("2024-01-01T00:00:00+00:00");
        request.setToDate("2024-12-31T23:59:59+00:00");

        ScheduleInstanceDTO expected = instance(1L, "daily");
        when(api.getAllByInterval(any())).thenReturn(List.of(expected));

        List<ScheduleInstanceDTO> result = service.getSchedules(request);

        assertThat(result).containsExactly(expected);
        verify(api).getAllByInterval(any());
        verify(api, never()).getAllByInterval(any(String.class), any());
    }

    @Test
    void getScheduleInstanceDAGDelegatesById() {
        SchedulerRestClientApi api = mock(SchedulerRestClientApi.class);
        ScheduleService service = new ScheduleService(api, mock(ConfigRestClientApi.class));

        ScheduleInstanceDAGDTO expected = new ScheduleInstanceDAGDTO();
        expected.setId(7L);
        expected.setConfigScheduleKeyName("daily");
        expected.setStatus(Status.Schedule.SUCCESS);
        when(api.getScheduleInstanceDAGDTOById(7L)).thenReturn(expected);

        ScheduleInstanceDAGDTO result = service.getScheduleInstanceDAG(7L);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getConfigScheduleKeyName()).isEqualTo("daily");
        verify(api).getScheduleInstanceDAGDTOById(7L);
    }

    private ScheduleInstanceDTO instance(Long id, String name) {
        ScheduleInstanceDTO dto = new ScheduleInstanceDTO();
        dto.setId(id);
        dto.setConfigScheduleKeyName(name);
        dto.setTargetExecutionDateTime("2024-06-01T10:00:00+00:00");
        dto.setStatus(Status.Schedule.SUCCESS);
        return dto;
    }
}