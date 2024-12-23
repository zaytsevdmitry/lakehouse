package org.lakehouse.config.rest.client;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lakehouse.cli.api.dto.configs.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lakehouse.cli.api.constant.Endpoint;


@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@RestClientTest(ClientApi.class)
@ContextConfiguration(classes = {TestConf.class, ClientApi.class})
@ActiveProfiles("test")
public class ClientApiTest {
	 
	@Autowired RestClient restClient;

	@Autowired ClientApi client = new ClientApi(restClient);

	@Autowired MockRestServiceServer server;

	@Autowired private ObjectMapper objectMapper;
	  
	 
	public ProjectDTO loadProjectDTO() throws IOException {
		return objectMapper.readValue(new File("../lakehouse-config-svc/demo/projects/demo.json"), ProjectDTO.class);
	}

	public ScheduleDTO loadScheduleDTO() throws IOException {
		return objectMapper.readValue(new File("../lakehouse-config-svc/demo/schedules/initial.json"), ScheduleDTO.class);
	}

	public ScenarioActTemplateDTO loadScenarioActTemplateDTO() throws IOException {
		return objectMapper.readValue(new File("../lakehouse-config-svc/demo/scenario-act-templates/default.json"), ScenarioActTemplateDTO.class);
	}


	@Test
	public void MakesCorrectCallProjectDTO() throws Exception {
		ProjectDTO expectProjectDTO = loadProjectDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.PROJECTS, expectProjectDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectProjectDTO), MediaType.APPLICATION_JSON));
		System.out.println("Project is loaded");


		ProjectDTO projectDTO = this.client.getProjectDTO(expectProjectDTO.getName());
		assert (expectProjectDTO.equals(projectDTO));
	}

	@Test
	public void MakesCorrectCallScheduleDTO() throws Exception {

		ScheduleDTO expectScheduleDTO = loadScheduleDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.SCHEDULES, expectScheduleDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectScheduleDTO), MediaType.APPLICATION_JSON));
		System.out.println("schedule is loaded");

		ScheduleDTO scheduleDTO = this.client.getScheduleDTO(expectScheduleDTO.getName());
		assert (expectScheduleDTO.equals(scheduleDTO));
	}

	@Test
	public void MakesCorrectCallScenarioActTemplateDTO() throws Exception {
		ScenarioActTemplateDTO scenarioActTemplateDTO = loadScenarioActTemplateDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.SCENARIOS, scenarioActTemplateDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(scenarioActTemplateDTO), MediaType.APPLICATION_JSON));
		System.out.println("scenario is loaded");

		ScenarioActTemplateDTO expect = loadScenarioActTemplateDTO();
		ScenarioActTemplateDTO result = this.client.getScenarioActTemplateDTO(expect.getName());
		assert (expect.equals(result));
	}


}
