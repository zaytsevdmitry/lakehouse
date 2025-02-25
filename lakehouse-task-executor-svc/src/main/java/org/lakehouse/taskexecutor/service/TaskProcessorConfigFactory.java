package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaskProcessorConfigFactory  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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



        result.setDataStores(getDataStores(result.getTargetDataSet(),result.getSources().values()));
        result.setScripts(result.getTargetDataSet()
                .getScripts()
                .stream()
                .sorted(Comparator.comparingInt(DataSetScriptDTO::getOrder ))
                .map(DataSetScriptDTO::getKey)
                .map(configRestClientApi::getScript)
                .toList());

        result.setKeyBind(getKeyBind(
                scheduledTaskLockDTO,
                result.getTargetDataSet(),
                result.getSources(),
                result.getDataStores()
        ));

        return result;
    }


    private Map<String,String> getKeyBind(
            ScheduledTaskLockDTO scheduledTaskLockDTO,
            DataSetDTO dataSetDTO,
            Map<String,DataSetDTO> sources,
            Map<String,DataStoreDTO> dataStores){
        Map<String,String> result = new HashMap<>();
        result.put("${target-timestamp-tz}",scheduledTaskLockDTO.getScheduleTargetDateTime());
        result.put("${data-set.name}", dataSetDTO.getName());


        List<DataSetDTO> srcAll = new ArrayList<>();
        srcAll.addAll(sources.values());
        srcAll.add(dataSetDTO);
        srcAll.forEach(src -> {
            result.put(String.format("${source(%s)}", dataSetDTO.getName()), dataSetDTO.getFullTableName());
        });
        result.entrySet().forEach(stringStringEntry ->
                logger.info("key -> {} | valuee -> {}",stringStringEntry.getKey(),stringStringEntry.getValue())
        );

        return result;
    }

    private Map<String, DataStoreDTO> getDataStores(DataSetDTO targetDataSet, Collection<DataSetDTO> sourceDatasets){
        Set<String> dataStoreNames = new HashSet<>();
        dataStoreNames.add(targetDataSet.getDataStore());
        dataStoreNames.addAll( sourceDatasets
                        .stream()
                        .map(DataSetDTO::getDataStore)
                        .collect(Collectors.toSet())
        );

        return dataStoreNames
                .stream()
                .map(configRestClientApi::getDataStoreDTO)
                .collect(Collectors.toMap(DataStoreDTO::getName, Function.identity()));
    }
}
