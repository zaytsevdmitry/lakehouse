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
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.scheduler.service.ScheduleInstanceDAGService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleInstanceDAGControllerTest {

    private ScheduleInstanceDAGService scheduleInstanceDAGService;
    private ScheduleInstanceDAGController controller;

    @BeforeEach
    void setUp() {
        scheduleInstanceDAGService = mock(ScheduleInstanceDAGService.class);
        controller = new ScheduleInstanceDAGController(scheduleInstanceDAGService);
    }

    @Test
    void getByIdDelegatesToService() {
        ScheduleInstanceDAGDTO expected = new ScheduleInstanceDAGDTO();
        expected.setId(1L);
        expected.setConfigScheduleKeyName("daily");
        expected.setTargetExecutionDateTime("2024-06-01T10:00:00+00:00");
        expected.setStatus(Status.Schedule.SUCCESS);
        when(scheduleInstanceDAGService.findById(1L)).thenReturn(expected);

        ScheduleInstanceDAGDTO result = controller.getById(1L);

        assertEquals(expected, result);
        verify(scheduleInstanceDAGService).findById(1L);
    }
}
