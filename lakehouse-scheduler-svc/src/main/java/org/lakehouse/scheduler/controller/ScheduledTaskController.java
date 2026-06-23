/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.scheduler.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScheduledTaskController {
    private final ScheduleTaskInstanceService scheduleTaskInstanceService;

    public ScheduledTaskController(ScheduleTaskInstanceService scheduleTaskInstanceService) {
        this.scheduleTaskInstanceService = scheduleTaskInstanceService;
    }

    @GetMapping(Endpoint.SCHEDULED_TASKS)
    List<ScheduledTaskDTO> getAll() {
        return scheduleTaskInstanceService.findAll();
    }

    @GetMapping(Endpoint.SCHEDULED_TASKS_ID)
    ScheduledTaskDTO getOne(@PathVariable Long id){
        return scheduleTaskInstanceService.findById(id);
    }

}
