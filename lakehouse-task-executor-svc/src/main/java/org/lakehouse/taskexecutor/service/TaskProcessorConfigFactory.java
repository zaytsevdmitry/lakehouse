package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.entity.TableDefinition;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskProcessorConfigFactory  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final ConfigRestClientApi configRestClientApi;
        private final TableDefinitionFactory tableDefinitionFactory;


    public TaskProcessorConfigFactory(ConfigRestClientApi configRestClientApi, TableDefinitionFactory tableDefinitionFactory) {
        this.configRestClientApi = configRestClientApi;
        this.tableDefinitionFactory = tableDefinitionFactory;
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

        Set<DataSetDTO> dataSets = collapseDataSetDTOs(result.getTargetDataSet(),result.getSources());

        result.setDataSetDTOSet(dataSets);

        result.setKeyBind(getKeyBind(scheduledTaskLockDTO));

        result.setTableDefinitions(getTableDefinitionMap(dataSets,result.getDataStores()));
        return result;
    }


    private Set<DataSetDTO> collapseDataSetDTOs(DataSetDTO dataSetDTO,
                                                Map<String,DataSetDTO> sources){

        Set<DataSetDTO> result = new HashSet<>();
        result.addAll(sources.values());
        result.add(dataSetDTO);
        return result;
    }

    private Map<String,String> getKeyBind(
            ScheduledTaskLockDTO scheduledTaskLockDTO){
        Map<String,String> result = new HashMap<>();
        result.put("${target-timestamp-tz}",scheduledTaskLockDTO.getScheduleTargetDateTime());



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

    private Map<String, TableDefinition> getTableDefinitionMap(
            Set<DataSetDTO> dataSetDTOS,
            Map<String,DataStoreDTO> dataStoreDTOMap){
        return dataSetDTOS
                .stream()
                .collect(
                        Collectors.toMap(
                                DataSetDTO::getName,
                                dataSetDTO -> tableDefinitionFactory
                                        .buildTableDefinition(
                                                dataSetDTO,
                                                dataStoreDTOMap.get(
                                                        dataSetDTO.getDataStore()))));


    }
}
