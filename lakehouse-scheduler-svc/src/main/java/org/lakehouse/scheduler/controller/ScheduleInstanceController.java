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

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.common.IntervalDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.service.ManageStateService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScheduleInstanceController {
    private final ManageStateService manageStateService;

    public ScheduleInstanceController(
            ScheduleTaskInstanceService scheduleInstanceService, ManageStateService manageStateService) {
        this.manageStateService = manageStateService;
    }

    @GetMapping(Endpoint.SCHEDULE)
    public List<ScheduleInstanceDTO> getAll() {
        return manageStateService.findAll();
    }

    @GetMapping(value = Endpoint.SCHEDULE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ScheduleInstanceDTO> getAllByInterval(
            @RequestParam(required = false) String name,
            @RequestBody IntervalDTO intervalDTO) {
        return manageStateService.findAllByInterval(
                name,
                DateTimeUtils.parseDateTimeFormatWithTZ(intervalDTO.getIntervalStartDateTime()),
                DateTimeUtils.parseDateTimeFormatWithTZ(intervalDTO.getIntervalEndDateTime()));
    }

    @GetMapping(Endpoint.SCHEDULE_NAME)
    public List<ScheduleInstanceDTO> getAllByName(@PathVariable String name, @PathVariable int limit) {
        return manageStateService.findAllByName(name, limit);
    }

    @DeleteMapping(Endpoint.SCHEDULE_ID)
    public void getAllByName(@PathVariable Long id) {
        manageStateService.delete(id);
    }


}
