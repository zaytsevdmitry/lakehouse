package org.lakehouse.test.config.api;

import org.apache.hc.core5.http.HttpStatus;
import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
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
    private final FileLoader fileLoader = new FileLoader();
    private final Map<String, TaskDTO> taskDTOEffectiveMap = new HashMap<>();
    private final Map<String, String> scriptMap;
    private final Map<String, DataSetDTO> dataSetDTOMap;
    private final Map<String, DataSourceDTO> dataStoreDTOMap;
    private final Map<String, DriverDTO> driverDTOMap;
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
                taskDTOEffectiveMap.put(key, taskDTO);
            });
        });

        this.dataSetDTOMap = fileLoader.loadAllDataSets();
        this.dataStoreDTOMap = fileLoader.loadAllDataSources();
        this.driverDTOMap = fileLoader.loadAllDrivers();
    }

    @Override
    public DriverDTO getDriverDTO(String name) {
        return driverDTOMap.get(name);
    }

    @Override
    public NameSpaceDTO getNameSpaceDTO(String NameSpaceName) {
        return null;
    }

    @Override
    public DataSourceDTO getDataSourceDTO(String name) {
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
        String key = String.format("%s %s %s", schedule, scenarioAct, task);
        if (!taskDTOEffectiveMap.containsKey(key)) {
            logger.error("Wrong key {}", key);
            taskDTOEffectiveMap.keySet().stream().sorted().forEach(string -> logger.info("Good key is {}", string));
        }
        return taskDTOEffectiveMap.get(key);
    }

    @Override
    public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name) {
        return null;
    }

    @Override
    public QualityMetricsConfDTO getQualityMetricsConf(String key) {
        return null;
    }

    @Override
    public String getScript(String key) {
        return scriptMap.get(key);
    }





    @Override
    public List<DriverDTO> getDriverDTOList() {
        return driverDTOMap.values().stream().toList();
    }

    @Override
    public List<NameSpaceDTO> getNameSpaceDTOList() {
        return List.of();
    }

    @Override
    public List<DataSourceDTO> getDataSourceDTOList() {
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
    public List<QualityMetricsConfDTO> getQualityMetricsConfList() {
        return List.of();
    }

    @Override
    public List<QualityMetricsConfDTO> getQualityMetricsConfList(String dataSetKeyName) {
        return List.of();
    }

    @Override
    public int deleteDriverDTO(String name) {
        return 0;
    }

    @Override
    public int deleteNameSpaceDTO(String NameSpaceName) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int deleteDataStoreDTO(String name) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int deleteDataSetDTO(String name) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int deleteScenarioActTemplateDTO(String name) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int deleteScheduleDTO(String name) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int deleteTaskExecutionServiceGroupDTO(String name) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postDriverDTO(DriverDTO o) {
        return 0;
    }

    @Override
    public int postNameSpaceDTO(NameSpaceDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postDataStoreDTO(DataSourceDTO dataSourceDTO) {
        dataStoreDTOMap.put(dataSourceDTO.getKeyName(), dataSourceDTO);
        return 200;
    }

    @Override
    public int postDataSetDTO(DataSetDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postScheduleDTO(ScheduleDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public int postQualityMetricsConf(QualityMetricsConfDTO o) {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }


}
