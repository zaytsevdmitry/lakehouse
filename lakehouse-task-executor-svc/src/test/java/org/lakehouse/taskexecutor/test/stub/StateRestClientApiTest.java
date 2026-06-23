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
