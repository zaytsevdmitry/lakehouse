package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
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
    ScheduleDTO get(@PathVariable String keyName) {
        return scheduleService.findDtoById(keyName);
    }

    @DeleteMapping(Endpoint.SCHEDULES_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        scheduleService.deleteById(keyName);
    }

    @GetMapping(Endpoint.EFFECTIVE_SCHEDULES_FROM_DT)
    List<ScheduleEffectiveDTO> getLastFromDate(@PathVariable String fromdt) {
        return scheduleService.findScheduleEffectiveDTOSByChangeDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(fromdt));
    }

    @GetMapping(Endpoint.EFFECTIVE_SCHEDULES_NAME)
    ScheduleEffectiveDTO getEffective(@PathVariable String keyName) {
        return scheduleService.findEffectiveScheduleDTOById(keyName);
    }

    @GetMapping(Endpoint.EFFECTIVE_SCHEDULE_SCENARIOACT_TASK)
    TaskDTO getEffectiveTaskDTO(
            @PathVariable String scheduleKeyName,
            @PathVariable String scenarioActName,
            @PathVariable String taskName
    ) {
        return scheduleService.getEffectiveTaskDTO(scheduleKeyName, scenarioActName, taskName);
    }
}
