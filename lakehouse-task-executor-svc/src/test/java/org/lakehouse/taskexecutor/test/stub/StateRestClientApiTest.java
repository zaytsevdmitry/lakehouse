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

package org.lakehouse.taskexecutor.test.stub;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;

import java.util.ArrayList;
import java.util.List;

public class StateRestClientApiTest implements StateRestClientApi {
    private final List<DataSetStateDTO> testCollection;

    public StateRestClientApiTest() {
        testCollection = new ArrayList<>();
    }

    public StateRestClientApiTest(List<DataSetStateDTO> testCollection) {
        this.testCollection = testCollection;
    }


    @Override
    public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO) {
        testCollection.add(dataSetStateDTO);
        return 200;
    }

    @Override
    public DataSetStateResponseDTO getDataSetStateResponseDTO(DataSetIntervalDTO dataSetIntervalDTO) {
        DataSetStateResponseDTO result = new DataSetStateResponseDTO();
        result.setWrongStates(
                testCollection
                        .stream()
                        .filter(dataSetStateDTO -> !dataSetStateDTO.getStatus().equals(Status.DataSet.SUCCESS.label))
                        .toList());
        return result;
    }
}
