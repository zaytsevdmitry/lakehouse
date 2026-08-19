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
package org.lakehouse.ui.service;

import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.ui.dto.DataSetStateRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StateService {

    private final StateRestClientApi stateRestClientApi;

    public StateService(StateRestClientApi stateRestClientApi) {
        this.stateRestClientApi = stateRestClientApi;
    }

    public List<DataSetStateDTO> getStates(DataSetStateRequestDTO request) {
        DataSetIntervalDTO interval = new DataSetIntervalDTO();
        interval.setDataSetKeyName(request.getDataSetKeyName());
        interval.setIntervalStartDateTime(request.getFromDate() + "T00:00:00Z");
        interval.setIntervalEndDateTime(request.getToDate() + "T23:59:59Z");
        return stateRestClientApi.getStatesByDataSetAndInterval(interval);
    }
}
