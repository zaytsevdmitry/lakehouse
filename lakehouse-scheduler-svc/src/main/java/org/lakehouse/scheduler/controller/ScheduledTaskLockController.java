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
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScheduledTaskLockController {
    private final ScheduleTaskInstanceService scheduleTaskInstanceService;

    public ScheduledTaskLockController(ScheduleTaskInstanceService scheduleTaskInstanceService) {
        this.scheduleTaskInstanceService = scheduleTaskInstanceService;
    }

    @GetMapping(Endpoint.SCHEDULED_TASKS_LOCK_ID)
    ScheduledTaskLockDTO getOne(@PathVariable String id) {
        return scheduleTaskInstanceService.getScheduledTaskLockDTO(id);
    }

    @GetMapping(Endpoint.SCHEDULED_TASKS_LOCK_BY_ID)
    ScheduledTaskLockDTO lockTaskById(@PathVariable Long id, @PathVariable String serviceId) {
        return scheduleTaskInstanceService.lockTaskById(id, serviceId);
    }

    @PutMapping(Endpoint.SCHEDULED_TASKS_LOCK_HEARTBEAT)
    void taskExecutionHeartBeat(@RequestBody TaskExecutionHeartBeatDTO taskExecutionHeartBeat) {
        scheduleTaskInstanceService.heartBeat(taskExecutionHeartBeat);
    }

    @PutMapping(Endpoint.SCHEDULED_TASKS_RELEASE)
    void releaseLock(@RequestBody TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
        scheduleTaskInstanceService.releaseTask(taskInstanceReleaseDTO);
    }


    @GetMapping(Endpoint.SCHEDULED_TASKS_LOCKS)
    List<ScheduledTaskLockDTO> getLocksAll() {
        return scheduleTaskInstanceService.getScheduledTaskLockDTOs();
    }

}
