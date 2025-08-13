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
public class StateRestClientTest {

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
