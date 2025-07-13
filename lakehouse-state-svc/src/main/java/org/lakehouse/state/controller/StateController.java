package org.lakehouse.state.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.service.StateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StateController {
    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping(Endpoint.STATE_DATASET)
    DataSetStateResponseDTO getStateByInterval(
            @RequestBody DataSetIntervalDTO dataSetIntervalDTO) {
        return stateService.getStateByInterval(
                dataSetIntervalDTO.getDataSetKeyName(),
                DateTimeUtils.parseDateTimeFormatWithTZ(dataSetIntervalDTO.getIntervalStartDateTime()),
                DateTimeUtils.parseDateTimeFormatWithTZ(dataSetIntervalDTO.getIntervalEndDateTime()));
    }
    @PutMapping(Endpoint.STATE_DATASET)
    void setState(@RequestBody  DataSetStateDTO dataSetStateDTO) {
        try {
            stateService.save(StateMapper.getState(dataSetStateDTO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
