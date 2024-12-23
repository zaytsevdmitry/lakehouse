package org.lakehouse.config.mapper;

import org.lakehouse.cli.api.dto.configs.TaskDTO;

import java.util.Map;

import org.lakehouse.config.entities.TaskAbstract;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

	public TaskDTO mapTaskToDTO(TaskAbstract taskAbstract, Map<String,String> executionModuleArgs) {
		TaskDTO taskDTO = new TaskDTO();
		taskDTO.setName(taskAbstract.getName());
		taskDTO.setDescription(taskAbstract.getDescription());
		taskDTO.setImportance(taskAbstract.getImportance());
		taskDTO.setExecutionModule(taskAbstract.getExecutionModule());
		taskDTO.setTaskExecutionServiceGroupName(taskAbstract.getTaskExecutionServiceGroup().getName());
		taskDTO.setExecutionModuleArgs(executionModuleArgs);
		return taskDTO;
	}
}
