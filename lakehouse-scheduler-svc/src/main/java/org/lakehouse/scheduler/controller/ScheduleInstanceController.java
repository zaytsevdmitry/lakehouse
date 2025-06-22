package org.lakehouse.scheduler.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.scheduler.service.ManageStateService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	List<ScheduleInstanceDTO> getAll() {
		return manageStateService.findAll();
	}

	@GetMapping(Endpoint.SCHEDULE_NAME)
	List<ScheduleInstanceDTO> getAllByName(@PathVariable String name, @PathVariable int limit) {
		return manageStateService.findAllByName(name,limit);
	}

	@DeleteMapping(Endpoint.SCHEDULE_ID)
	void getAllByName(@PathVariable Long id) {
		 manageStateService.delete(id);
	}

	
}
