package org.lakehouse.test.config.api;

import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.test.config.configuration.FileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigRestClientApiTest implements ConfigRestClientApi {
    private final int HTTP_NOT_IMPLEMENTED_501 = 501;
    private final FileLoader fileLoader = new FileLoader();
    private final Map<String, TaskDTO> taskDTOEffectiveMap = new HashMap<>();
    private final Map<String,String> scriptMap;
    private final Map<String,DataSetDTO> dataSetDTOMap;
    private final Map<String,DataStoreDTO> dataStoreDTOMap;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public ConfigRestClientApiTest() throws IOException {
        scriptMap = fileLoader.loadAllModelScripts();
        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO();
        sef.getScenarioActs().forEach(scheduleScenarioActEffectiveDTO -> {
            scheduleScenarioActEffectiveDTO.getTasks().forEach(taskDTO -> {
                String key = String.format(
                        "%s %s %s",
                        sef.getName(),
                        scheduleScenarioActEffectiveDTO.getName(),
                        taskDTO.getName());
                taskDTOEffectiveMap.put(key,taskDTO);
            });
        });

        this.dataSetDTOMap = fileLoader.loadAllDataSets();
        this.dataStoreDTOMap = fileLoader.loadAllDataStores();

    }

    @Override
    public ProjectDTO getProjectDTO(String ProjectName) {
        return null;
    }

    @Override
    public DataStoreDTO getDataStoreDTO(String name) {
        return dataStoreDTOMap.get(name);
    }

    @Override
    public DataSetDTO getDataSetDTO(String name) {
        return dataSetDTOMap.get(name);
    }

    @Override
    public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name) {
        return null;
    }

    @Override
    public ScheduleDTO getScheduleDTO(String name) {
        return null;
    }

    @Override
    public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name) {
        try {
            return fileLoader.loadScheduleEffectiveDTO();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public TaskDTO getEffectiveTaskDTO(String schedule, String scenarioAct, String task) {
        String key = String.format("%s %s %s",schedule,scenarioAct,task);
        if(!taskDTOEffectiveMap.containsKey(key)) {
            logger.error("Wrong key {}", key);
            taskDTOEffectiveMap.keySet().stream().sorted().forEach(string -> logger.info("Good key is {}",string));
        }
        return taskDTOEffectiveMap.get(key);
    }

    @Override
    public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name) {
        return null;
    }

    @Override
    public String getScript(String key) {
        return scriptMap.get(key);
    }

    @Override
    public List<ProjectDTO> getProjectDTOList() {
        return List.of();
    }

    @Override
    public List<DataStoreDTO> getDataStoreDTOList() {
        return List.of();
    }

    @Override
    public List<DataSetDTO> getDataSetDTOList() {
        return List.of();
    }

    @Override
    public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList() {
        return List.of();
    }

    @Override
    public List<ScheduleDTO> getScheduleDTOList() {
        return List.of();
    }

    @Override
    public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt) {
        return List.of();
    }

    @Override
    public List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList() {
        return List.of();
    }

    @Override
    public int deleteProjectDTO(String ProjectName) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int deleteDataStoreDTO(String name) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int deleteDataSetDTO(String name) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int deleteScenarioActTemplateDTO(String name) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int deleteScheduleDTO(String name) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int deleteTaskExecutionServiceGroupDTO(String name) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int postProjectDTO(ProjectDTO o) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int postDataStoreDTO(DataStoreDTO dataStoreDTO) {
        dataStoreDTOMap.put(dataStoreDTO.getName(), dataStoreDTO);
        return 200;
    }

    @Override
    public int postDataSetDTO(DataSetDTO o) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int postScheduleDTO(ScheduleDTO o) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }

    @Override
    public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
         return HTTP_NOT_IMPLEMENTED_501;  
    }



}
