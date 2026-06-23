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

package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.config.service.TaskExecutionServiceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskExecutionServiceGroupController {
    private final TaskExecutionServiceGroupService taskExecutionServiceGroupService;

    public TaskExecutionServiceGroupController(TaskExecutionServiceGroupService taskExecutionServiceGroupService) {
        this.taskExecutionServiceGroupService = taskExecutionServiceGroupService;
    }

    @GetMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUPS)
    List<TaskExecutionServiceGroupDTO> findAll() {
        return taskExecutionServiceGroupService.findAll();
    }

    @PostMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUPS)
    @ResponseStatus(HttpStatus.CREATED)
    TaskExecutionServiceGroupDTO post(@RequestBody TaskExecutionServiceGroupDTO taskExecutionServiceGroup) {
        return taskExecutionServiceGroupService.save(taskExecutionServiceGroup);
    }

    @GetMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME)
    TaskExecutionServiceGroupDTO get(@PathVariable String keyName) {
        return taskExecutionServiceGroupService.findById(keyName);
    }

    @DeleteMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        taskExecutionServiceGroupService.deleteById(keyName);
    }
}
