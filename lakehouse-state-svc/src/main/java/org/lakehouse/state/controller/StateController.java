package org.lakehouse.state.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.service.StateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StateController {
    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping(Endpoint.STATE_DATASET_GET)
    List<DataSetStateDTO> get(
            @PathVariable String dataSetKeyName,
            @PathVariable String intervalStartDateTime,
            @PathVariable String intervalEndDateTime) {
        return stateService.get(
                dataSetKeyName,
                DateTimeUtils.parceDateTimeFormatWithTZ(intervalStartDateTime),
                DateTimeUtils.parceDateTimeFormatWithTZ(intervalEndDateTime)).stream().map(StateMapper::getDataSetStateDTO).toList();
    }
    @PutMapping(Endpoint.SCHEDULED_TASKS_LOCK_HEARTBEAT)
    void setState(@RequestBody  DataSetStateDTO dataSetStateDTO) {
        try {
            stateService.save(StateMapper.getState(dataSetStateDTO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
