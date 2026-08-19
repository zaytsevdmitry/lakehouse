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

import org.lakehouse.client.api.dto.common.IntervalDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.ui.dto.ScheduleRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class ScheduleService {

    private final SchedulerRestClientApi schedulerRestClientApi;
    private final ConfigRestClientApi configRestClientApi;

    public ScheduleService(
            SchedulerRestClientApi schedulerRestClientApi,
            ConfigRestClientApi configRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.configRestClientApi = configRestClientApi;
    }

    public List<ScheduleInstanceDTO> getSchedules(ScheduleRequestDTO request) {
        IntervalDTO interval = new IntervalDTO();
        interval.setIntervalStartDateTime(request.getFromDate());
        interval.setIntervalEndDateTime(request.getToDate());

        List<String> names = request.getNames();
        if (names == null || names.isEmpty()) {
            return schedulerRestClientApi.getAllByInterval(interval);
        }
        return names.stream()
                .flatMap(name -> schedulerRestClientApi.getAllByInterval(name, interval).stream())
                .toList();

    }

    public List<ScheduleHeaderDTO> getScheduleHeaders() {
        return configRestClientApi.getScheduleHeaderDTOList();
    }

    public ScheduleInstanceDAGDTO getScheduleInstanceDAG(Long id) {
        return schedulerRestClientApi.getScheduleInstanceDAGDTOById(id);
    }
}
