package org.lakehouse.client.rest.state;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.RestClientHelper;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.rest.state.StateRestClientApi;

import java.util.HashMap;
import java.util.Map;


public class StateRestClientApiImpl implements StateRestClientApi {
	
	private final RestClientHelper restClientHelper;
	
	public StateRestClientApiImpl(RestClientHelper restClientHelper) {
		this.restClientHelper = restClientHelper;
	}


	@Override
	public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO) {
		return restClientHelper.putDTO(dataSetStateDTO, Endpoint.STATE_DATASET);
	}

	@Override
	public DataSetStateDTO getDataSetStateDTO(DataSetStateDTO dataSetStateDTO) {
		return restClientHelper.getDtoOne(
				Map.of(
						"dataSetKeyName",dataSetStateDTO.getDataSetKeyName(),
						"intervalStartDateTime", dataSetStateDTO.getIntervalStartDateTime(),
						"intervalEndDateTime", dataSetStateDTO.getIntervalEndDateTime()
				),
				Endpoint.STATE_DATASET_GET,
				DataSetStateDTO.class);
	}
}