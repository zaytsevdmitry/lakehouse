package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.*;
import org.lakehouse.client.api.dto.task.SourceConfDTO;

import java.time.OffsetDateTime;
import java.util.List;


public interface ConfigRestClientApi {


    DriverDTO getDriverDTO(String name);

    NameSpaceDTO getNameSpaceDTO(String NameSpaceName);

    DataSourceDTO getDataSourceDTO(String name);

    DataSetDTO getDataSetDTO(String name);

    ScenarioActTemplateDTO getScenarioActTemplateDTO(String name);

    ScheduleDTO getScheduleDTO(String name);

    ScheduleEffectiveDTO getScheduleEffectiveDTO(String name);

    TaskDTO getEffectiveTaskDTO(String schedule, String scenarioAct, String task);

    TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name);

    QualityMetricsConfDTO getQualityMetricsConf(String key);

    String getScript(String key);

    SourceConfDTO getSourceConfDTO(String dataSetKeyName);

    String getDataSetModelScript(String dataSetKeyName);

    List<DriverDTO> getDriverDTOList();

    List<NameSpaceDTO> getNameSpaceDTOList();

    List<DataSourceDTO> getDataSourceDTOList();

    List<DataSetDTO> getDataSetDTOList();

    List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList();

    List<ScheduleDTO> getScheduleDTOList();

    List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt);

    List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList();

    List<QualityMetricsConfDTO> getQualityMetricsConfList();

    List<QualityMetricsConfDTO> getQualityMetricsConfList(String dataSetKeyName);

    int deleteDriverDTO(String name);

    int deleteNameSpaceDTO(String NameSpaceName);

    int deleteDataStoreDTO(String name);

    int deleteDataSetDTO(String name);

    int deleteScenarioActTemplateDTO(String name);

    int deleteScheduleDTO(String name);

    int deleteTaskExecutionServiceGroupDTO(String name);

    int postDriverDTO(DriverDTO o);

    int postNameSpaceDTO(NameSpaceDTO o);

    int postDataStoreDTO(DataSourceDTO o);

    int postDataSetDTO(DataSetDTO o);

    int postScenarioActTemplateDTO(ScenarioActTemplateDTO o);

    int postScheduleDTO(ScheduleDTO o);

    int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o);

    int postQualityMetricsConf(QualityMetricsConfDTO o);
}

