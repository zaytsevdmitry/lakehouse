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

package org.lakehouse.client.rest.state.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.client.rest.state.configuration.StateRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {StateRestClientConfiguration.class})
@RestClientTest(properties = {
        "lakehouse.client.rest.state.server.url=",
})
public class StateRestClientHelperTest {

    @Autowired
    StateRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSetStateDTO getDataSetStateDTOExpect() {
        DataSetStateDTO dataSetStateDTOExpect = new DataSetStateDTO();
        dataSetStateDTOExpect.setDataSetKeyName("test");
        dataSetStateDTOExpect.setStatus(Status.DataSet.SUCCESS);
        dataSetStateDTOExpect.setIntervalStartDateTime("2025-03-01T00:00:00.0Z");
        dataSetStateDTOExpect.setIntervalEndDateTime("2025-03-02T00:00:00.0Z");
        return dataSetStateDTOExpect;
    }

    private DataSetStateDTO getRequestDataSetStateDTO() {
        DataSetStateDTO dataSetStateDTOExpect = getDataSetStateDTOExpect();
        DataSetStateDTO dataSetStateDTORequest = new DataSetStateDTO();
        dataSetStateDTORequest.setDataSetKeyName(dataSetStateDTOExpect.getDataSetKeyName());
        dataSetStateDTORequest.setStatus(null);
        dataSetStateDTORequest.setIntervalStartDateTime(dataSetStateDTOExpect.getIntervalStartDateTime());
        dataSetStateDTORequest.setIntervalEndDateTime(dataSetStateDTOExpect.getIntervalEndDateTime());
        return dataSetStateDTORequest;
    }

    @Test
    public void MakesCorrectCallPutDataSetState() throws Exception {


        DataSetStateDTO dataSetStateDTOExpect = getDataSetStateDTOExpect();

        server.expect(requestTo(Endpoint.STATE_DATASET))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(objectMapper.writeValueAsString(dataSetStateDTOExpect)))
                .andRespond(withSuccess());

        client.setDataSetStateDTO(dataSetStateDTOExpect);
    }

    @Test
    public void MakesCorrectCallGetDataSetState() throws Exception {

        DataSetIntervalDTO interval = new DataSetIntervalDTO();
        interval.setDataSetKeyName("test");
        interval.setIntervalStartDateTime("2025-03-01T00:00:00.0Z");
        interval.setIntervalEndDateTime("2025-03-02T00:00:00.0Z");

        DataSetStateResponseDTO expectResponse = new DataSetStateResponseDTO();

        server.expect(ExpectedCount.manyTimes(),
                        requestTo(Endpoint.STATE_DATASET))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(interval)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectResponse), MediaType.APPLICATION_JSON));

        DataSetStateResponseDTO result = client.getDataSetStateResponseDTO(interval);

        assert (result.equals(expectResponse));
    }

}
