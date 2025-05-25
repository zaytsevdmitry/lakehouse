package org.lakehouse.client.rest.state.test;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.runner.RunWith;
import org.lakehouse.client.api.constant.Status;
import org.junit.Test;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.state.configuration.StateRestClientConfiguration;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lakehouse.client.api.constant.Endpoint;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {StateRestClientConfiguration.class})
@RestClientTest(properties = {
		"lakehouse.client.rest.state.server.url=",
})
public class StateRestClientHelperTest {

	@Autowired
	StateRestClientApi client ;

	@Autowired MockRestServiceServer server;

	@Autowired private ObjectMapper objectMapper;

	private DataSetStateDTO getDataSetStateDTOExpect(){
		DataSetStateDTO dataSetStateDTOExpect = new DataSetStateDTO();
		dataSetStateDTOExpect.setDataSetKeyName("test");
		dataSetStateDTOExpect.setStatus(Status.DataSet.SUCCESS.label);
		dataSetStateDTOExpect.setIntervalStartDateTime("2025-03-01T00:00:00.0Z");
		dataSetStateDTOExpect.setIntervalEndDateTime("2025-03-02T00:00:00.0Z");
		return dataSetStateDTOExpect;
	}

	private DataSetStateDTO getRequestDataSetStateDTO(){
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

		DataSetStateDTO dataSetStateDTOExpect = getDataSetStateDTOExpect();

		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/dataSetKeyName/%s/intervalStartDateTime/%s/intervalEndDateTime/%s",
								Endpoint.STATE_DATASET,
								dataSetStateDTOExpect.getDataSetKeyName(),
								dataSetStateDTOExpect.getIntervalStartDateTime(),
								dataSetStateDTOExpect.getIntervalEndDateTime())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(dataSetStateDTOExpect), MediaType.APPLICATION_JSON));

		DataSetStateDTO result = client.getDataSetStateDTO(getRequestDataSetStateDTO());

		assert (result.equals(dataSetStateDTOExpect));
	}

}
