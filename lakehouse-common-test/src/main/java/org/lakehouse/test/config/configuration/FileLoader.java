package org.lakehouse.test.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.api.dto.configs.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class FileLoader {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String rootPath = "../lakehouse-config-svc/demo";
	public final String modelsDir = rootPath.concat("/dataset-sql-model");
	public final String datastoresDir = rootPath.concat("/datastores");


	private List<String> getFilenames(String directoryName){
		return Arrays
				.stream(Objects.requireNonNull(new File(directoryName).listFiles()))
				.map(File::getName)
				.map(fullName -> fullName.split("\\.")[0])
				.toList();

	}

	public ProjectDTO loadProjectDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/projects/demo.json")), ProjectDTO.class);
	}

	public TaskExecutionServiceGroupDTO loadTaskExecutionServiceGroupDTO() throws IOException {
		return objectMapper.readValue(new File(rootPath.concat("/taskexecutionservicegroups/default.json")),
				TaskExecutionServiceGroupDTO.class);
	}

	public DataStoreDTO loadDataStoreDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format(datastoresDir.concat("/%s.json"), name)), DataStoreDTO.class);

	}

	public Map<String,DataStoreDTO> loadAllDataStores() {
		return getFilenames(datastoresDir)
				.stream()
				.map(name -> {
                    try {
                        return loadDataStoreDTO(name);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toMap(DataStoreDTO::getName, Function.identity()));
	}

	public ScenarioActTemplateDTO loadScenarioDTO() throws IOException {
		return objectMapper
				.readValue(
						new File(
								rootPath.concat("/scenario-act-templates/default.json")), ScenarioActTemplateDTO.class);
	}

	private final String dataSetDir = rootPath.concat("/datasets");
	public DataSetDTO loadDataSetDTO(String name) throws IOException {
		return objectMapper.readValue(new File(String.format(dataSetDir.concat("/%s.json"), name)), DataSetDTO.class);
	}

	public Map<String,DataSetDTO> loadAllDataSets(){
		return getFilenames(dataSetDir)
				.stream()
				.filter(string -> !string.equals("transaction_dds_v2"))
				.map(name -> {
					try {
                		return loadDataSetDTO(name);
            		} catch (IOException e) {
                		throw new RuntimeException(e);
					}})
				.collect(
						Collectors
								.toMap(
										DataSetDTO::getName,
										dataSetDTO -> dataSetDTO));
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
		File file = new File(String.format(modelsDir.concat("/%s.sql"), name));


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
		for(String name : getFilenames(modelsDir)
		){
			result.put(name.concat(".sql"),loadModelScript(name));
		}
		return result;
	}
}
