package lakehouse.api.controller;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.ScheduleDTO;
import lakehouse.api.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }


    @GetMapping(Endpoint.SCHEDULE)
    List<ScheduleDTO> findAll() {
        return scheduleService.findAll();
    }

    @PostMapping(Endpoint.SCHEDULE)
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleDTO post(@RequestBody ScheduleDTO schedule) {
        return scheduleService.save(schedule);
    }

    @GetMapping(Endpoint.SCHEDULE_NAME)
    ScheduleDTO get(@PathVariable String name) {
        return scheduleService.findById(name);
    }


    @DeleteMapping(Endpoint.SCHEDULE_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        scheduleService.deleteById(name);
    }
}
