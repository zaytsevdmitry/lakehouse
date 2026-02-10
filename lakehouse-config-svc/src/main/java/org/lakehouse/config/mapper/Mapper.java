package org.lakehouse.config.mapper;

import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.config.entities.TaskAbstract;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Mapper {

    public TaskDTO mapTaskToDTO(TaskAbstract taskAbstract, Map<String, String> executionModuleArgs) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setName(taskAbstract.getName());
        taskDTO.setDescription(taskAbstract.getDescription());
        taskDTO.setImportance(taskAbstract.getImportance());
        taskDTO.setTaskProcessor(taskAbstract.getTaskProcessor());
        taskDTO.setTaskProcessorBody(taskAbstract.getTaskProcessorBody());
        taskDTO.setTaskExecutionServiceGroupName(taskAbstract.getTaskExecutionServiceGroup().getKeyName());
        taskDTO.setTaskProcessorArgs(executionModuleArgs);
        return taskDTO;
    }



}
