package org.lakehouse.test.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.api.dto.configs.*;

import java.io.File;
import java.io.IOException;


public class FileLoader {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String rootPath = "../lakehouse-config-svc/demo";


	public ProjectDTO loadProjectDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/projects/demo.json")), ProjectDTO.class);
	}

	public TaskExecutionServiceGroupDTO loadTaskExecutionServiceGroupDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/taskexecutionservicegroups/default.json")),
				TaskExecutionServiceGroupDTO.class);
	}

	public DataStoreDTO loadDataStoreDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format(rootPath.concat("/datastores/%s.json"), name)), DataStoreDTO.class);
	}

	public ScenarioActTemplateDTO loadScenarioDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/scenario-act-templates/default.json")),
				ScenarioActTemplateDTO.class);
	}
/*
	public <T> T stringToObject(String string, Class<T> clazz) throws IOException {
		return objectMapper.readValue(string, clazz);
	}
**/
	public DataSetDTO loadDataSetDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format(rootPath.concat("/datasets/%s.json"), name)), DataSetDTO.class);
	}

	public ScheduleDTO loadScheduleDTO(String name) throws Exception {
		return objectMapper.readValue(new File(String.format(rootPath.concat("/schedules/%s.json"), name)), ScheduleDTO.class);
	}

	public ScenarioActTemplateDTO loadScenarioActTemplateDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/scenario-act-templates/default.json")), ScenarioActTemplateDTO.class);

	}
	public ScheduleEffectiveDTO loadScheduleEffectiveDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/schedules_effective/initial.json")), ScheduleEffectiveDTO.class);
	}

}
