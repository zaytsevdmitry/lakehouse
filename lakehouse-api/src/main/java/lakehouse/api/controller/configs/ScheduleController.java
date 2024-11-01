package lakehouse.api.controller.configs;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.configs.ScheduleDTO;
import lakehouse.api.service.configs.ScheduleService;
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
		return scheduleService.findById(name);
	}

	@DeleteMapping(Endpoint.SCHEDULES_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		scheduleService.deleteById(name);
	}
}
