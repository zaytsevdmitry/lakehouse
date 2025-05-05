package org.lakehouse.scheduler.controller;

import java.util.List;

import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;

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
		return scheduleTaskInstanceService.lockTaskById(id,serviceId);
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
