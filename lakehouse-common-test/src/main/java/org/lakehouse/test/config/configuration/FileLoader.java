package org.lakehouse.test.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.api.dto.configs.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


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

	public String loadModelScript(String name) throws IOException {
		String result = null;
		File file = new File(String.format(rootPath.concat("/models/%s.sql"), name));


		DataInputStream reader = new DataInputStream(new FileInputStream(file));
		int nBytesToRead = reader.available();
		if(nBytesToRead > 0) {
			byte[] bytes = new byte[nBytesToRead];
			reader.read(bytes);
			result = new String(bytes);
		}

		return result;
	}
	public Map<String,String> loadAllModelScripts() throws IOException{
		Map<String,String> result= new HashMap<>();
		for(String name : Arrays
				.stream(Objects.requireNonNull(new File(rootPath.concat("/models")).listFiles()))
				.map(File::getName)
				.map(fullName -> fullName.replaceAll(".sql",""))
				.toList()
		){
			result.put(name,loadModelScript(name));
		}
		return result;
	}
}
