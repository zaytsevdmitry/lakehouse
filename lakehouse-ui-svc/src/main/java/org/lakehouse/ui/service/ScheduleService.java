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
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.ui.dto.ScheduleRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final SchedulerRestClientApi schedulerRestClientApi;

    public ScheduleService(SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
    }

    public List<ScheduleInstanceDTO> getSchedules(ScheduleRequestDTO request) {
        IntervalDTO interval = new IntervalDTO();
        interval.setIntervalStartDateTime(request.getFromDate());
        interval.setIntervalEndDateTime(request.getToDate());
        return schedulerRestClientApi.getAllByInterval(interval);
    }
}
