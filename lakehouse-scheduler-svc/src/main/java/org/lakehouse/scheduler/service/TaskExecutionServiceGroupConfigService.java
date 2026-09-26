package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaskExecutionServiceGroupConfigService {
    private final Map<String, TaskExecutionServiceGroupDTO> serviceGroupDTOMap = new HashMap<>();
    private final ConfigRestClientApi configRestClientApi;

    public TaskExecutionServiceGroupConfigService(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public void addTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO){
        serviceGroupDTOMap.put(taskExecutionServiceGroupDTO.getName(), taskExecutionServiceGroupDTO);
    }

    public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name){
        if (!serviceGroupDTOMap.containsKey(name))
                serviceGroupDTOMap.put(name, configRestClientApi.getTaskExecutionServiceGroupDTO(name));

        return serviceGroupDTOMap.get(name);
    }
    public void deleteTaskExecutionServiceGroupDTO(String name){
        if (serviceGroupDTOMap.containsKey(name))
            serviceGroupDTOMap.remove(name);
    }
}
