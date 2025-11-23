package org.lakehouse.client.rest.state;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.rest.RestClientHelper;


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
    public DataSetStateResponseDTO getDataSetStateResponseDTO(DataSetIntervalDTO dataSetIntervalDTO) {
        return restClientHelper.getDtoOne(
                dataSetIntervalDTO,
				/*Map.of(
						"dataSetKeyName",dataSetIntervalDTO.getDataSetKeyName(),
						"intervalStartDateTime", dataSetIntervalDTO.getIntervalStartDateTime(),
						"intervalEndDateTime", dataSetIntervalDTO.getIntervalEndDateTime()
				),*/
                Endpoint.STATE_DATASET,
                DataSetStateResponseDTO.class);
    }
}