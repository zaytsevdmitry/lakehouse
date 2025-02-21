package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaskProcessorConfigFactory  {

        private final ConfigRestClientApi configRestClientApi;

    public TaskProcessorConfigFactory(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public TaskProcessorConfig buildTaskProcessorConfig(ScheduledTaskLockDTO scheduledTaskLockDTO){
        TaskProcessorConfig result = new TaskProcessorConfig();
        result.setTargetDataSet(
                configRestClientApi.getDataSetDTO(
                scheduledTaskLockDTO.getDataSetKeyName()));
        result.setExecutionModuleArgs(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getExecutionModuleArgs());
        result.setSources(
                result.getTargetDataSet().getSources().stream()
                        .map(dataSetSourceDTO -> {
                            DataSetDTO srcds = configRestClientApi.getDataSetDTO(dataSetSourceDTO.getName());
                            srcds.getProperties().putAll(dataSetSourceDTO.getProperties());
                            return srcds;
                        }).collect(Collectors.toMap(DataSetDTO::getName, Function.identity())));
        result.setDataStores(
                Stream.of(
                result.getTargetDataSet().getDataStore(), result.getSources()
                        .keySet())
                .collect(Collectors.toSet())
                .stream().map(o-> configRestClientApi.getDataStoreDTO(o.toString()))
                .collect(Collectors.toMap(DataStoreDTO::getName,Function.identity()))
        );
      //todo  result.setScripts(?);
        return result;
    }

}
