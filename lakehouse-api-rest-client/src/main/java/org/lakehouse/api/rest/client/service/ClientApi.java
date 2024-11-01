package org.lakehouse.api.rest.client.service;
import lakehouse.api.constant.Endpoint;

import lakehouse.api.dto.configs.DataSetDTO;
import lakehouse.api.dto.configs.DataStoreDTO;
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.dto.configs.ScenarioActTemplateDTO;
import lakehouse.api.dto.configs.ScheduleDTO;
import lakehouse.api.dto.configs.TaskExecutionServiceGroupDTO;
import lakehouse.api.dto.tasks.ScheduledTaskDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Service
public class ClientApi{
	
	private final RestClient restClient;
	
	public ClientApi(RestClient restClient) {
		this.restClient = restClient;
	}

	private  <T> T getDtoByName(String dtoName, String urn, Class<T> clazz) {
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
		return getDtoByName(ProjectName, Endpoint.PROJECTS_NAME, ProjectDTO.class);
	}
	
	public DataStoreDTO getDataStoreDTO(String name) {
		return getDtoByName(name,  Endpoint.DATA_STORES_NAME, DataStoreDTO.class);
	}
	
	public DataSetDTO getDataSetDTO(String name) {
		return getDtoByName(name,  Endpoint.DATA_SETS_NAME, DataSetDTO.class);
	}
	
	public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name) {
		return getDtoByName(name,  Endpoint.SCENARIOS_NAME, ScenarioActTemplateDTO.class);
	}
	
	public ScheduleDTO getScheduleDTO(String name) {
		return getDtoByName(name,  Endpoint.SCHEDULES_NAME, ScheduleDTO.class);
	}
	
	public TaskExecutionServiceGroupDTO  getTaskExecutionServiceGroupDTO(String name) {
		return getDtoByName(name,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME, TaskExecutionServiceGroupDTO.class);
	}

	public ScheduledTaskDTO  getScheduledTaskDTO(String name) {
		return getDtoByName(name,  Endpoint.SCHEDULED_TASKS_ID, ScheduledTaskDTO.class);
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
	
	public int putProjectDTO(ProjectDTO o) {
		return putDTO(o, Endpoint.PROJECTS_NAME);
	}
	
	public int putDataStoreDTO(DataStoreDTO o) {
		return putDTO(o,  Endpoint.DATA_STORES_NAME);
	}
	
	public int putDataSetDTO(DataSetDTO o) {
		return putDTO(o,  Endpoint.DATA_SETS_NAME);
	}
	
	public int putScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
		return putDTO(o,  Endpoint.SCENARIOS_NAME);
	}
	
	public int putScheduleDTO(ScheduleDTO o) {
		return putDTO(o,  Endpoint.SCHEDULES_NAME);
	}
	
	public int  putTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
		return putDTO(o,  Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
	}

	public int  putScheduledTaskDTO(ScheduledTaskDTO o) {
		return putDTO(o,  Endpoint.SCHEDULED_TASKS_ID);
	}	
}