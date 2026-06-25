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
