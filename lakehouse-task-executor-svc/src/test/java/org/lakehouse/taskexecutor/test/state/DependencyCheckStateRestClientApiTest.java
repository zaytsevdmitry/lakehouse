package org.lakehouse.taskexecutor.test.state;

import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;

import java.util.ArrayList;
import java.util.List;

public class DependencyCheckStateRestClientApiTest implements StateRestClientApi {
    private final List<DataSetStateDTO> testCollection;

    public DependencyCheckStateRestClientApiTest() {
        testCollection = new ArrayList<>();
    }
    public DependencyCheckStateRestClientApiTest(List<DataSetStateDTO> testCollection) {
        this.testCollection = testCollection;
    }


    @Override
    public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO) {
        return 200;
    }

    @Override
    public DataSetStateResponseDTO getDataSetStateResponseDTO(DataSetIntervalDTO dataSetIntervalDTO) {
        DataSetStateResponseDTO result = new DataSetStateResponseDTO();
        result.setWrongStates(testCollection);
        return result;
    }
}
