package lakehouse.api.controller;

import lakehouse.api.dto.ScheduleDTO;
import lakehouse.api.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }


    @GetMapping("/schedules")
    List<ScheduleDTO> findAll() {
        return scheduleService.findAll();
    }

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleDTO post(@RequestBody ScheduleDTO schedule) {
        return scheduleService.save(schedule);
    }

    @GetMapping("/schedules/{name}")
    ScheduleDTO get(@PathVariable String name) {
        return scheduleService.findById(name);
    }


    @DeleteMapping("/schedules/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        scheduleService.deleteById(name);
    }
}
