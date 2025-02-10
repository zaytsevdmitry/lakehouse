package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.ScheduleDTO;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.config.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScheduleController {
	private final ScheduleService scheduleService;

	public ScheduleController(ScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}

	@GetMapping(Endpoint.SCHEDULES)
	List<ScheduleDTO> findAll() {
		return scheduleService.findAll();
	}

	@PostMapping(Endpoint.SCHEDULES)
	@ResponseStatus(HttpStatus.CREATED)
	ScheduleDTO post(@RequestBody ScheduleDTO schedule) {
		return scheduleService.save(schedule);
	}

	@GetMapping(Endpoint.SCHEDULES_NAME)
	ScheduleDTO get(@PathVariable String name) {
		return scheduleService.findDtoById(name);
	}

	@DeleteMapping(Endpoint.SCHEDULES_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		scheduleService.deleteById(name);
	}

	@GetMapping(Endpoint.EFFECTIVE_SCHEDULES_FROM_DT)
	List<ScheduleEffectiveDTO> getLastFromDate(@PathVariable String fromdt) {
		return scheduleService.findScheduleEffectiveDTOSByChangeDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(fromdt));
	}

	@GetMapping(Endpoint.EFFECTIVE_SCHEDULES_NAME)
	ScheduleEffectiveDTO getEffective(@PathVariable String name) {
		return scheduleService.findEffectiveScheduleDTOById(name);
	}

	@GetMapping(Endpoint.EFFECTIVE_SCHEDULE_SCENARIOACT_TASK)
	TaskDTO getEffectiveTaskDTO(
			@PathVariable String schedule,
			@PathVariable String scenarioAct,
			@PathVariable String task
	){
		return scheduleService.getEffectiveTaskDTO(schedule,scenarioAct,task);
	}
}
