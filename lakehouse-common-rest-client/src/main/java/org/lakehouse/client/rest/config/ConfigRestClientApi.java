package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.dto.configs.*;

import java.time.OffsetDateTime;
import java.util.List;


public interface ConfigRestClientApi {


	public ProjectDTO getProjectDTO(String ProjectName);
	public DataStoreDTO getDataStoreDTO(String name);
	
	public DataSetDTO getDataSetDTO(String name);
	public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name);
	
	public ScheduleDTO getScheduleDTO(String name);
	public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name);
	public TaskDTO getEffectiveTaskDTO(String schedule, String scenarioAct, String task);
	public TaskExecutionServiceGroupDTO  getTaskExecutionServiceGroupDTO(String name);

	public List<ProjectDTO> getProjectDTOList();
	public List<DataStoreDTO> getDataStoreDTOList();
	
	public List<DataSetDTO> getDataSetDTOList() ;
	
	public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList();
	public List<ScheduleDTO> getScheduleDTOList() ;
	public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt);
	public List<TaskExecutionServiceGroupDTO>  getTaskExecutionServiceGroupDTOList() ;


	public int deleteProjectDTO(String ProjectName);
	public int deleteDataStoreDTO(String name);
	public int deleteDataSetDTO(String name);
	public int deleteScenarioActTemplateDTO(String name);
	public int deleteScheduleDTO(String name);
	
	public int  deleteTaskExecutionServiceGroupDTO(String name) ;


	public int postProjectDTO(ProjectDTO o);
	public int postDataStoreDTO(DataStoreDTO o);
	
	public int postDataSetDTO(DataSetDTO o);
	
	public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o);
	public int postScheduleDTO(ScheduleDTO o);
	public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o);
}

