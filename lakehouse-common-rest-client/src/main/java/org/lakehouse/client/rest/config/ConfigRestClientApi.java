package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

import java.time.OffsetDateTime;
import java.util.List;


public interface ConfigRestClientApi {


    public NameSpaceDTO getNameSpaceDTO(String NameSpaceName);

    public DataSourceDTO getDataSourceDTO(String name);

    public DataSetDTO getDataSetDTO(String name);

    public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name);

    public ScheduleDTO getScheduleDTO(String name);

    public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name);

    public TaskDTO getEffectiveTaskDTO(String schedule, String scenarioAct, String task);

    public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name);

    public QualityMetricsConfDTO getQualityMetricsConf(String key);

    public String getScript(String key);

    public List<NameSpaceDTO> getNameSpaceDTOList();

    public List<DataSourceDTO> getDataSourceDTOList();

    public List<DataSetDTO> getDataSetDTOList();

    public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList();

    public List<ScheduleDTO> getScheduleDTOList();

    public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt);

    public List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList();

    public List<QualityMetricsConfDTO> getQualityMetricsConfList();

    public List<QualityMetricsConfDTO> getQualityMetricsConfList(String dataSetKeyName);

    public int deleteNameSpaceDTO(String NameSpaceName);

    public int deleteDataStoreDTO(String name);

    public int deleteDataSetDTO(String name);

    public int deleteScenarioActTemplateDTO(String name);

    public int deleteScheduleDTO(String name);

    public int deleteTaskExecutionServiceGroupDTO(String name);


    public int postNameSpaceDTO(NameSpaceDTO o);

    public int postDataStoreDTO(DataSourceDTO o);

    public int postDataSetDTO(DataSetDTO o);

    public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o);

    public int postScheduleDTO(ScheduleDTO o);

    public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o);

    public int postQualityMetricsConf(QualityMetricsConfDTO o);
}

