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

package org.lakehouse.scheduler.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.common.IntervalDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.scheduler.service.ManageStateService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleInstanceControllerTest {

    private ManageStateService manageStateService;
    private ScheduleInstanceController controller;

    @BeforeEach
    void setUp() {
        manageStateService = mock(ManageStateService.class);
        ScheduleTaskInstanceService scheduleTaskInstanceService = mock(ScheduleTaskInstanceService.class);
        controller = new ScheduleInstanceController(scheduleTaskInstanceService, manageStateService);
    }

    @Test
    void getAllByIntervalParsesIntervalAndDelegatesToService() {
        IntervalDTO intervalDTO = new IntervalDTO();
        intervalDTO.setIntervalStartDateTime("2024-01-01T00:00:00+00:00");
        intervalDTO.setIntervalEndDateTime("2024-12-31T23:59:59+00:00");

        ScheduleInstanceDTO expected = new ScheduleInstanceDTO();
        expected.setId(1L);
        expected.setConfigScheduleKeyName("daily");
        expected.setTargetExecutionDateTime("2024-06-01T10:00:00+00:00");
        expected.setStatus(Status.Schedule.SUCCESS);
        when(manageStateService.findAllByInterval(any(), any())).thenReturn(List.of(expected));

        List<ScheduleInstanceDTO> result = controller.getAllByInterval(intervalDTO);

        assertEquals(1, result.size());
        assertEquals(expected, result.get(0));

        ArgumentCaptor<OffsetDateTime> startCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(manageStateService).findAllByInterval(startCaptor.capture(), endCaptor.capture());
        assertEquals(OffsetDateTime.parse("2024-01-01T00:00:00+00:00"), startCaptor.getValue());
        assertEquals(OffsetDateTime.parse("2024-12-31T23:59:59+00:00"), endCaptor.getValue());
    }

    @Test
    void getAllByIntervalReturnsEmptyListWhenNoInstances() {
        IntervalDTO intervalDTO = new IntervalDTO();
        intervalDTO.setIntervalStartDateTime("2024-01-01T00:00:00+00:00");
        intervalDTO.setIntervalEndDateTime("2024-01-02T00:00:00+00:00");
        when(manageStateService.findAllByInterval(any(), any())).thenReturn(List.of());

        List<ScheduleInstanceDTO> result = controller.getAllByInterval(intervalDTO);

        assertEquals(0, result.size());
        verify(manageStateService).findAllByInterval(any(), any());
    }
}
