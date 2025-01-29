package org.lakehouse.client.rest.config;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.utils.DateTimeUtils;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;


public class ConfigRestClientApiImpl implements ConfigRestClientApi {
	
	private final RestClientHelper restClientHelper;
	
	public ConfigRestClientApiImpl(RestClientHelper restClientHelper) {
		this.restClientHelper = restClientHelper;
	}
	
 
	public ProjectDTO getProjectDTO(String ProjectName) {
		return restClientHelper.getDtoOne(ProjectName, Endpoint.PROJECTS_NAME, ProjectDTO.class);
	}
	
	public DataStoreDTO getDataStoreDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.DATA_STORES_NAME, DataStoreDTO.class);
	}
	
	public DataSetDTO getDataSetDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.DATA_SETS_NAME, DataSetDTO.class);
	}
	
	public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.SCENARIOS_NAME, ScenarioActTemplateDTO.class);
	}
	
	public ScheduleDTO getScheduleDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.SCHEDULES_NAME, ScheduleDTO.class);
	}

	public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.EFFECTIVE_SCHEDULES_NAME, ScheduleEffectiveDTO.class);
	}

	public TaskExecutionServiceGroupDTO  getTaskExecutionServiceGroupDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME, TaskExecutionServiceGroupDTO.class);
	}

	public ScheduledTaskDTO  getScheduledTaskDTO(String name) {
		return restClientHelper.getDtoOne(name,  Endpoint.SCHEDULED_TASKS_ID, ScheduledTaskDTO.class);
	}

	public ScheduledTaskLockDTO  getScheduledTaskLockDTO(String id) {
		return restClientHelper.getDtoOne(id,  Endpoint.SCHEDULED_TASKS_LOCK_ID, ScheduledTaskLockDTO.class);
	}

	public List<ProjectDTO> getProjectDTOList(){
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.PROJECTS)
				.retrieve()
				.body(ProjectDTO[].class)); //getDtoByName("",  Endpoint.PROJECTS, List.class);
	}
	public List<DataStoreDTO> getDataStoreDTOList() {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.DATA_STORES)
				.retrieve()
				.body(DataStoreDTO[].class));
	}
	
	public List<DataSetDTO> getDataSetDTOList() {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.DATA_SETS)
				.retrieve()
				.body(DataSetDTO[].class));
	}
	
	public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList() {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.SCENARIOS)
				.retrieve()
				.body(ScenarioActTemplateDTO[].class));
	}
	
	public List<ScheduleDTO> getScheduleDTOList() {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.SCHEDULES)
				.retrieve()
				.body(ScheduleDTO[].class));
	}
	public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt) {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(
						Endpoint.EFFECTIVE_SCHEDULES_FROM_DT,
						DateTimeUtils.formatDateTimeFormatWithTZ(dt))
				.retrieve()
				.body(ScheduleEffectiveDTO[].class));
	}

	public List<TaskExecutionServiceGroupDTO>  getTaskExecutionServiceGroupDTOList() {
		return Arrays.asList( restClientHelper.getRestClient()
				.get()
				.uri(Endpoint.TASK_EXECUTION_SERVICE_GROUPS)
				.retrieve()
				.body(TaskExecutionServiceGroupDTO[].class));
	}


	
	public int deleteProjectDTO(String ProjectName) {
		return restClientHelper.deleteDtoByName(ProjectName, Endpoint.PROJECTS_NAME);
	}
	
	public int deleteDataStoreDTO(String name) {
		return restClientHelper.deleteDtoByName(name,  Endpoint.DATA_STORES_NAME);
	}
	
	public int deleteDataSetDTO(String name) {
		return restClientHelper.deleteDtoByName(name,  Endpoint.DATA_SETS_NAME);
	}
	
	public int deleteScenarioActTemplateDTO(String name) {
		return restClientHelper.deleteDtoByName(name,  Endpoint.SCENARIOS_NAME);
	}
	
	public int deleteScheduleDTO(String name) {
		return restClientHelper.deleteDtoByName(name,  Endpoint.SCHEDULES_NAME);
	}
	
	public int  deleteTaskExecutionServiceGroupDTO(String name) {
		return restClientHelper.deleteDtoByName(name,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
	}


	
	public int postProjectDTO(ProjectDTO o) {
		return restClientHelper.postDTO(o, Endpoint.PROJECTS);
	}
	
	public int postDataStoreDTO(DataStoreDTO o) {
		return restClientHelper.postDTO(o,  Endpoint.DATA_STORES);
	}
	
	public int postDataSetDTO(DataSetDTO o) {
		return restClientHelper.postDTO(o,  Endpoint.DATA_SETS);
	}
	
	public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
		return restClientHelper.postDTO(o,  Endpoint.SCENARIOS);
	}
	
	public int postScheduleDTO(ScheduleDTO o) {
		return restClientHelper.postDTO(o,  Endpoint.SCHEDULES);
	}
	
	public int  postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
		return restClientHelper.postDTO(o,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS);
	}


}