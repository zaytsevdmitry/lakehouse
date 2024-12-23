package org.lakehouse.config.rest.client.service;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lakehouse.cli.api.dto.configs.*;
import org.lakehouse.cli.api.utils.Coalesce;
import org.lakehouse.cli.api.utils.DateTimeUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.lakehouse.cli.api.constant.Endpoint;
import org.lakehouse.cli.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.cli.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.cli.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.cli.api.dto.tasks.ScheduledTaskDTO;

@Service
public class ClientApi{
	
	private final RestClient restClient;
	
	public ClientApi(RestClient restClient) {
		this.restClient = restClient;
	}

	private  <T> T getDtoOne(String dtoName, String urn, Class<T> clazz) {
		return restClient
				.get()
				.uri(urn, dtoName)
				.retrieve()
				.body(clazz);		
	}

	/*
	 * private <T> List<T> getDtoList(String urn, Class<T> clazz){ return (List<T>)
	 * restClient .get() .uri(urn) .retrieve() .body(List.class).stream().map(o ->
	 * clazz.cast(o)).toList(); }
	 */
 
	public ProjectDTO getProjectDTO(String ProjectName) {
		return getDtoOne(ProjectName, Endpoint.PROJECTS_NAME, ProjectDTO.class);
	}
	
	public DataStoreDTO getDataStoreDTO(String name) {
		return getDtoOne(name,  Endpoint.DATA_STORES_NAME, DataStoreDTO.class);
	}
	
	public DataSetDTO getDataSetDTO(String name) {
		return getDtoOne(name,  Endpoint.DATA_SETS_NAME, DataSetDTO.class);
	}
	
	public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name) {
		return getDtoOne(name,  Endpoint.SCENARIOS_NAME, ScenarioActTemplateDTO.class);
	}
	
	public ScheduleDTO getScheduleDTO(String name) {
		return getDtoOne(name,  Endpoint.SCHEDULES_NAME, ScheduleDTO.class);
	}

	public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name) {
		return getDtoOne(name,  Endpoint.EFFECTIVE_SCHEDULES_NAME, ScheduleEffectiveDTO.class);
	}

	public TaskExecutionServiceGroupDTO  getTaskExecutionServiceGroupDTO(String name) {
		return getDtoOne(name,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME, TaskExecutionServiceGroupDTO.class);
	}

	public ScheduledTaskDTO  getScheduledTaskDTO(String name) {
		return getDtoOne(name,  Endpoint.SCHEDULED_TASKS_ID, ScheduledTaskDTO.class);
	}

	public ScheduledTaskLockDTO  getScheduledTaskLockDTO(String id) {
		return getDtoOne(id,  Endpoint.SCHEDULED_TASKS_LOCK_ID, ScheduledTaskLockDTO.class);
	}

	public List<ProjectDTO> getProjectDTOList(){
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.PROJECTS)
				.retrieve()
				.body(ProjectDTO[].class)); //getDtoByName("",  Endpoint.PROJECTS, List.class);
	}
	public List<DataStoreDTO> getDataStoreDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.DATA_STORES)
				.retrieve()
				.body(DataStoreDTO[].class));
	}
	
	public List<DataSetDTO> getDataSetDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.DATA_SETS)
				.retrieve()
				.body(DataSetDTO[].class));
	}
	
	public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.SCENARIOS)
				.retrieve()
				.body(ScenarioActTemplateDTO[].class));
	}
	
	public List<ScheduleDTO> getScheduleDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.SCHEDULES)
				.retrieve()
				.body(ScheduleDTO[].class));
	}
	public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt) {
		return Arrays.asList( restClient
				.get()
				.uri(
						Endpoint.EFFECTIVE_SCHEDULES_FROM_DT,
						DateTimeUtils.formatDateTimeFormatWithTZ(dt))
				.retrieve()
				.body(ScheduleEffectiveDTO[].class));
	}

	public List<TaskExecutionServiceGroupDTO>  getTaskExecutionServiceGroupDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.TASK_EXECUTION_SERVICE_GROUPS)
				.retrieve()
				.body(TaskExecutionServiceGroupDTO[].class));
	}

	public List<ScheduledTaskDTO>  getScheduledTaskDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.SCHEDULED_TASKS)
				.retrieve()
				.body(ScheduledTaskDTO[].class));
	}

	public List<ScheduledTaskLockDTO>  getScheduledTaskLockDTOList() {
		return Arrays.asList( restClient
				.get()
				.uri(Endpoint.SCHEDULED_TASKS_LOCKS)
				.retrieve()
				.body(ScheduledTaskLockDTO[].class));
	}
	
	private int deleteDtoByName(String id, String urn) {
		return restClient
				.delete()
				.uri(urn, id)
				.retrieve()
				.toBodilessEntity()
				.getStatusCode()
				.value();
	}
	
	public int deleteProjectDTO(String ProjectName) {
		return deleteDtoByName(ProjectName, Endpoint.PROJECTS_NAME);
	}
	
	public int deleteDataStoreDTO(String name) {
		return deleteDtoByName(name,  Endpoint.DATA_STORES_NAME);
	}
	
	public int deleteDataSetDTO(String name) {
		return deleteDtoByName(name,  Endpoint.DATA_SETS_NAME);
	}
	
	public int deleteScenarioActTemplateDTO(String name) {
		return deleteDtoByName(name,  Endpoint.SCENARIOS_NAME);
	}
	
	public int deleteScheduleDTO(String name) {
		return deleteDtoByName(name,  Endpoint.SCHEDULES_NAME);
	}
	
	public int  deleteTaskExecutionServiceGroupDTO(String name) {
		return deleteDtoByName(name,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
	}

	public int  deleteScheduledTaskDTO(String name) {
		return deleteDtoByName(name,  Endpoint.SCHEDULED_TASKS_ID);
	}
	
	private int putDTO(Object o, String urn) {
		return restClient.put()
		  .uri(urn)
		  .contentType(MediaType.APPLICATION_JSON)
		  .body(o)
		  .retrieve()
		  .toBodilessEntity().getStatusCode().value();
	}
	
	private int postDTO(Object o, String urn) {
		return restClient.post()
		  .uri(urn)
		  .contentType(MediaType.APPLICATION_JSON)
		  .body(o)
		  .retrieve()
		  .toBodilessEntity().getStatusCode().value();
	}
	
	public int postProjectDTO(ProjectDTO o) {
		return postDTO(o, Endpoint.PROJECTS);
	}
	
	public int postDataStoreDTO(DataStoreDTO o) {
		return postDTO(o,  Endpoint.DATA_STORES);
	}
	
	public int postDataSetDTO(DataSetDTO o) {
		return postDTO(o,  Endpoint.DATA_SETS);
	}
	
	public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
		return postDTO(o,  Endpoint.SCENARIOS);
	}
	
	public int postScheduleDTO(ScheduleDTO o) {
		return postDTO(o,  Endpoint.SCHEDULES);
	}
	
	public int  postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
		return postDTO(o,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS);
	}

	public int  postScheduledTaskDTO(ScheduledTaskDTO o) {
		return postDTO(o,  Endpoint.SCHEDULED_TASKS_ID);
	}
	
	public ScheduledTaskLockDTO lockTask(String taskExecutionServiceGroupName, String serviceId) {
		return restClient
				.get()
				.uri(Endpoint.SCHEDULED_TASKS_LOCK, taskExecutionServiceGroupName, serviceId)
				.retrieve()
				.body(ScheduledTaskLockDTO.class);	
	}
	
	public int lockHeartBeat (TaskExecutionHeartBeatDTO taskExecutionHeartBeat) {
		return putDTO(taskExecutionHeartBeat, Endpoint.SCHEDULED_TASKS_LOCK_HEARTBEAT);
	}
	
	public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
		return putDTO(taskInstanceReleaseDTO, Endpoint.SCHEDULED_TASKS_RELEASE);
	}

}