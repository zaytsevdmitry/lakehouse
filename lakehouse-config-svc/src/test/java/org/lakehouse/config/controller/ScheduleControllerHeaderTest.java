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

package org.lakehouse.config.controller;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.lakehouse.config.service.ScheduleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleControllerHeaderTest {

    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final ScheduleController controller = new ScheduleController(scheduleService);

    @Test
    void findAllHeadersDelegatesToService() {
        ScheduleHeaderDTO header = new ScheduleHeaderDTO();
        header.setKeyName("daily");
        header.setIntervalExpression("0 0 0 * * *");
        when(scheduleService.findAllHeaders()).thenReturn(List.of(header));

        List<ScheduleHeaderDTO> result = controller.findAllHeaders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyName()).isEqualTo("daily");
        verify(scheduleService).findAllHeaders();
    }

    @Test
    void findAllHeadersReturnsEmptyListWhenNoHeaders() {
        when(scheduleService.findAllHeaders()).thenReturn(List.of());

        List<ScheduleHeaderDTO> result = controller.findAllHeaders();

        assertThat(result).isEmpty();
        verify(scheduleService).findAllHeaders();
    }
}
