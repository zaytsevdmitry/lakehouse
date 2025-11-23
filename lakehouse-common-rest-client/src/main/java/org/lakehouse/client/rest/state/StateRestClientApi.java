package org.lakehouse.client.rest.state;

import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;


public interface StateRestClientApi {
    public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO);

    public DataSetStateResponseDTO getDataSetStateResponseDTO(DataSetIntervalDTO dataSetIntervalDTO);

}
