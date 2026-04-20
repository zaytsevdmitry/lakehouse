package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.*;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.RestClientHelper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;


public class ConfigRestClientApiImpl extends ConfigRestClientApiAbstract {

    private final RestClientHelper restClientHelper;

    public ConfigRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }


    @Override
    public DriverDTO getDriverDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.DRIVERS_NAME, DriverDTO.class);
    }

    public NameSpaceDTO getNameSpaceDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.NAME_SPACES_NAME, NameSpaceDTO.class);
    }

    public DataSourceDTO getDataSourceDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.DATA_SOURCES_NAME, DataSourceDTO.class);
    }

    public DataSetDTO getDataSetDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.DATA_SETS_NAME, DataSetDTO.class);
    }

    public ScenarioActTemplateDTO getScenarioActTemplateDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.SCENARIOS_NAME, ScenarioActTemplateDTO.class);
    }

    public ScheduleDTO getScheduleDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.SCHEDULES_NAME, ScheduleDTO.class);
    }

    public ScheduleEffectiveDTO getScheduleEffectiveDTO(String keyName) {
        return restClientHelper.getDtoOne(keyName, Endpoint.EFFECTIVE_SCHEDULES_NAME, ScheduleEffectiveDTO.class);
    }

    @Override
    public TaskDTO getEffectiveTaskDTO(String scheduleKeyName, String scenarioActName, String taskName) {
        return restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.EFFECTIVE_SCHEDULE_SCENARIOACT_TASK, scheduleKeyName, scenarioActName, taskName)
                .retrieve()
                .body(TaskDTO.class);
    }

    public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name) {
        return restClientHelper.getDtoOne(name, Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME, TaskExecutionServiceGroupDTO.class);
    }

    @Override
    public QualityMetricsConfDTO getQualityMetricsConf(String key) {
        return null;
    }

    public ScheduledTaskMsgDTO getScheduledTaskDTO(String name) {
        return restClientHelper.getDtoOne(name, Endpoint.SCHEDULED_TASKS_ID, ScheduledTaskMsgDTO.class);
    }

    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id) {
        return restClientHelper.getDtoOne(id, Endpoint.SCHEDULED_TASKS_LOCK_ID, ScheduledTaskLockDTO.class);
    }

    public String getScript(String key) {
        return restClientHelper.getDtoOne(key, Endpoint.SCRIPT_BY_KEY, String.class);
    }
    public SourceConfDTO getSourceConfDTO(String dataSetKeyName) {
        return restClientHelper.getDtoOne(dataSetKeyName, Endpoint.SOURCES_CONF_BY_DATASET_KEY_NAME, SourceConfDTO.class);
    }

    @Override
    public String getDataSetModelScript(String dataSetKeyName) {
        return restClientHelper.getDtoOne(dataSetKeyName, Endpoint.DATASET_MODEL_SCRIPT_BY_DATASET_KEY_NAME, String.class);

    }

    @Override
    public List<DriverDTO> getDriverDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.DRIVERS)
                .retrieve()
                .body(DriverDTO[].class));
    }

    public List<NameSpaceDTO> getNameSpaceDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.NAME_SPACES)
                .retrieve()
                .body(NameSpaceDTO[].class)); //getDtoByName("",  Endpoint.PROJECTS, List.class);
    }

    public List<DataSourceDTO> getDataSourceDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.DATA_SOURCES)
                .retrieve()
                .body(DataSourceDTO[].class));
    }

    public List<DataSetDTO> getDataSetDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.DATA_SETS)
                .retrieve()
                .body(DataSetDTO[].class));
    }

    public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.SCENARIOS)
                .retrieve()
                .body(ScenarioActTemplateDTO[].class));
    }

    public List<ScheduleDTO> getScheduleDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.SCHEDULES)
                .retrieve()
                .body(ScheduleDTO[].class));
    }

    public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt) {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(
                        Endpoint.EFFECTIVE_SCHEDULES_FROM_DT,
                        DateTimeUtils.formatDateTimeFormatWithTZ(dt))
                .retrieve()
                .body(ScheduleEffectiveDTO[].class));
    }

    public List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.TASK_EXECUTION_SERVICE_GROUPS)
                .retrieve()
                .body(TaskExecutionServiceGroupDTO[].class));
    }

    @Override
    public List<QualityMetricsConfDTO> getQualityMetricsConfList() {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.QUALITY_METRICS)
                .retrieve()
                .body(QualityMetricsConfDTO[].class));
    }

    @Override
    public List<QualityMetricsConfDTO> getQualityMetricsConfListByDataSetKeyName(String dataSetKeyName) {
        return Arrays.asList(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.QUALITY_METRICS_BY_DATASET, dataSetKeyName)
                .retrieve()
                .body(QualityMetricsConfDTO[].class));
    }

    @Override
    public int deleteDriverDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.DRIVERS_NAME);
    }


    public int deleteNameSpaceDTO(String NameSpaceName) {
        return restClientHelper.deleteDtoByName(NameSpaceName, Endpoint.NAME_SPACES_NAME);
    }

    public int deleteDataStoreDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.DATA_SOURCES_NAME);
    }

    public int deleteDataSetDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.DATA_SETS_NAME);
    }

    public int deleteScenarioActTemplateDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.SCENARIOS_NAME);
    }

    public int deleteScheduleDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.SCHEDULES_NAME);
    }

    public int deleteTaskExecutionServiceGroupDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
    }

    @Override
    public int postDriverDTO(DriverDTO o) {
        return restClientHelper.postDTO(o, Endpoint.DRIVERS);
    }


    public int postNameSpaceDTO(NameSpaceDTO o) {
        return restClientHelper.postDTO(o, Endpoint.NAME_SPACES);
    }

    public int postDataStoreDTO(DataSourceDTO o) {
        return restClientHelper.postDTO(o, Endpoint.DATA_SOURCES);
    }

    public int postDataSetDTO(DataSetDTO o) {
        return restClientHelper.postDTO(o, Endpoint.DATA_SETS);
    }

    public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
        return restClientHelper.postDTO(o, Endpoint.SCENARIOS);
    }

    public int postScheduleDTO(ScheduleDTO o) {
        return restClientHelper.postDTO(o, Endpoint.SCHEDULES);
    }

    public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
        return restClientHelper.postDTO(o, Endpoint.TASK_EXECUTION_SERVICE_GROUPS);
    }

    @Override
    public int postQualityMetricsConf(QualityMetricsConfDTO o) {
        return 0;
    }


}