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
