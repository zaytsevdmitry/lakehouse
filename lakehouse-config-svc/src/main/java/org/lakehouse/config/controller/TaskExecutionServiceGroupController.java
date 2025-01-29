package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.TaskExecutionServiceGroupDTO;

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
	TaskExecutionServiceGroupDTO get(@PathVariable String name) {
		return taskExecutionServiceGroupService.findById(name);
	}

	@DeleteMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		taskExecutionServiceGroupService.deleteById(name);
	}
}
