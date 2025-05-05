package org.lakehouse.client.rest.config.test;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.runner.RunWith;
import org.lakehouse.client.api.dto.configs.*;
import org.junit.Test;

import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.lakehouse.client.api.constant.Endpoint;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ConfigRestClientConfiguration.class})
@RestClientTest(properties = {
		"lakehouse.client.rest.config.server.url=",
})
public class ConfigRestClientHelperTest {

	@Autowired
	ConfigRestClientApi client ;

	@Autowired MockRestServiceServer server;

	@Autowired private ObjectMapper objectMapper;


	private final FileLoader fileLoader = new FileLoader();

	@Test
	public void MakesCorrectCallProjectDTO() throws Exception {
		ProjectDTO expectProjectDTO = fileLoader.loadProjectDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.PROJECTS, expectProjectDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectProjectDTO), MediaType.APPLICATION_JSON));
		System.out.println("Project is loaded");


		ProjectDTO projectDTO = this.client.getProjectDTO(expectProjectDTO.getName());
		assert (expectProjectDTO.equals(projectDTO));
	}

	@Test
	public void MakesCorrectCallScheduleDTO() throws Exception {

		ScheduleDTO expectScheduleDTO = fileLoader.loadScheduleDTO("initial");
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.SCHEDULES, expectScheduleDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectScheduleDTO), MediaType.APPLICATION_JSON));
		System.out.println("schedule is loaded");

		ScheduleDTO scheduleDTO = this.client.getScheduleDTO(expectScheduleDTO.getName());
		assert (expectScheduleDTO.equals(scheduleDTO));
	}

	@Test
	public void MakesCorrectCallScenarioActTemplateDTO() throws Exception {
		ScenarioActTemplateDTO scenarioActTemplateDTO = fileLoader.loadScenarioActTemplateDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/%s", Endpoint.SCENARIOS, scenarioActTemplateDTO.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(scenarioActTemplateDTO), MediaType.APPLICATION_JSON));
		System.out.println("scenario is loaded");

		ScenarioActTemplateDTO expect = fileLoader.loadScenarioActTemplateDTO();
		ScenarioActTemplateDTO result = client.getScenarioActTemplateDTO(expect.getName());
		assert (expect.equals(result));
	}

	@Test
	public void MakesCorrectCallScheduleEffectiveDTO() throws Exception {
		ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO();
		server.expect(ExpectedCount.manyTimes(),
						requestTo(String.format("%s/name/%s", Endpoint.EFFECTIVE_SCHEDULES_ROOT, sef.getName())))
				.andRespond(withSuccess(objectMapper.writeValueAsString(sef), MediaType.APPLICATION_JSON));
		System.out.println("Schedule effective is loaded");

		ScheduleEffectiveDTO expect = fileLoader.loadScheduleEffectiveDTO();
		ScheduleEffectiveDTO result = client.getScheduleEffectiveDTO(expect.getName());
		assert (expect.equals(result));
	}

	@Test
	public void MakesCorrectCallScheduleEffectiveTaskDTO() throws Exception {
		TaskDTO mergeTaskDTOExpected = new TaskDTO();
		Map<String,String> mergeTaskDTOExpectedArgs = new HashMap<>();
		mergeTaskDTOExpectedArgs.put("spark.executor.memory", "5gb");
		mergeTaskDTOExpectedArgs.put("spark.driver.memory", "2gb");
		mergeTaskDTOExpectedArgs.put("spark.driver.cores", "3");
		mergeTaskDTOExpected.setExecutionModuleArgs(mergeTaskDTOExpectedArgs);
		mergeTaskDTOExpected.setName("merge");
		mergeTaskDTOExpected.setTaskExecutionServiceGroupName("default");
		mergeTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.datamanipulation.MergeProcessor");
		mergeTaskDTOExpected.setImportance("critical");
		mergeTaskDTOExpected.setDescription("load from remote datastore");

		server.expect(ExpectedCount.manyTimes(),
						requestTo( Endpoint.EFFECTIVE_SCHEDULE_SCENARIOACT_TASK
												.replaceAll("\\{schedule}", "initial")
												.replaceAll("\\{scenarioact}","transaction_dds" )
												.replaceAll("\\{task}","merge")

										))
				.andRespond(withSuccess(objectMapper.writeValueAsString(mergeTaskDTOExpected), MediaType.APPLICATION_JSON));
		System.out.println("Schedule effective is loaded");

		TaskDTO result = client.getEffectiveTaskDTO("initial",
				"transaction_dds",
				"merge");
		assert (mergeTaskDTOExpected.equals(result));
	}
}
