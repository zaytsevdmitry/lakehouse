package org.lakehouse.scheduler.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
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
	
}
