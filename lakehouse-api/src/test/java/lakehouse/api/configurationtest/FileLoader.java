package lakehouse.api.configurationtest;

import lakehouse.api.dto.configs.*;

import org.springframework.boot.test.context.TestComponent;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

@TestComponent
public class FileLoader {

	private final ObjectMapper objectMapper = new ObjectMapper();



	public ProjectDTO loadProjectDTO() throws IOException {
		return objectMapper.readValue(new File("demo/projects/demo.json"), ProjectDTO.class);
	}

	public TaskExecutionServiceGroupDTO loadTaskExecutionServiceGroupDTO() throws IOException {
		return objectMapper.readValue(new File("demo/taskexecutionservicegroups/default.json"),
				TaskExecutionServiceGroupDTO.class);
	}

	public DataStoreDTO loadDataStoreDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format("demo/datastores/%s.json", name)), DataStoreDTO.class);
	}

	public ScenarioActTemplateDTO loadScenarioDTO() throws IOException {
		return objectMapper.readValue(new File("demo/scenario-act-templates/default.json"),
				ScenarioActTemplateDTO.class);
	}
/*
	public <T> T stringToObject(String string, Class<T> clazz) throws IOException {
		return objectMapper.readValue(string, clazz);
	}
**/
	public DataSetDTO loadDataSetDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format("demo/datasets/%s.json", name)), DataSetDTO.class);
	}

	public ScheduleDTO loadScheduleDTO(String name) throws Exception {
		return objectMapper.readValue(new File(String.format("demo/schedules/%s.json", name)), ScheduleDTO.class);
	}
}
