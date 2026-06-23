/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
