package org.lakehouse.client.rest.state;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;

import java.time.OffsetDateTime;


public interface StateRestClientApi {
	public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO);

	public DataSetStateDTO getDataSetStateDTO(DataSetStateDTO dataSetStateDTO);

}

