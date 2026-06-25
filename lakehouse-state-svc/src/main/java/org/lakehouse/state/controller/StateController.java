/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    void setState(@RequestBody DataSetStateDTO dataSetStateDTO) {
        try {
            stateService.save(StateMapper.getState(dataSetStateDTO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
