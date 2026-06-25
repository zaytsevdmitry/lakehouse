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