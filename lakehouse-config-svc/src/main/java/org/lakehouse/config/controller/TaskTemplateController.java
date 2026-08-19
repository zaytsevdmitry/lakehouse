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
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.config.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskTemplateController {
    private final TaskService taskService;

    public TaskTemplateController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping(Endpoint.TASKS_TEMPLATE)
    List<TaskDTO> findAll() {
        return taskService.findAll();
    }

    @PostMapping(Endpoint.TASKS_TEMPLATE)
    @ResponseStatus(HttpStatus.CREATED)
    TaskDTO post(@RequestBody TaskDTO taskDTO) {
        return taskService.save(taskDTO, null, null).taskDTO();
    }

    @GetMapping(Endpoint.TASKS_TEMPLATE_NAME)
    TaskDTO get(@PathVariable String name) {
        return taskService.findByName(name, null, null);
    }

    @DeleteMapping(Endpoint.TASKS_TEMPLATE_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        taskService.deleteByName(name, null,null);
    }
}
