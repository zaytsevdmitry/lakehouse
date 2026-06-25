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
