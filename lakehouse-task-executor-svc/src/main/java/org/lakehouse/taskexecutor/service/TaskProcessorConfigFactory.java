package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.LogPasswdRespectively;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.entity.TableDefinition;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskProcessorConfigFactory  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConfigRestClientApi configRestClientApi;
    private final TableDefinitionFactory tableDefinitionFactory;
    private final Jinjava jinjava;
    public TaskProcessorConfigFactory(
            ConfigRestClientApi configRestClientApi,
            TableDefinitionFactory tableDefinitionFactory, Jinjava jinjava) {
        this.configRestClientApi = configRestClientApi;
        this.tableDefinitionFactory = tableDefinitionFactory;
        this.jinjava = jinjava;
    }

    public TaskProcessorConfig buildTaskProcessorConfig(ScheduledTaskLockDTO scheduledTaskLockDTO){
        TaskProcessorConfig result = new TaskProcessorConfig();

        result.setTargetDataSet(
                configRestClientApi.getDataSetDTO(
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName()));
        result.setExecutionModuleArgs(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getExecutionModuleArgs());
        result.setSources(
                result.getTargetDataSet().getSources().stream()
                        .map(dataSetSourceDTO -> {
                            DataSetDTO srcds = configRestClientApi.getDataSetDTO(dataSetSourceDTO.getName());
                            srcds.getProperties().putAll(dataSetSourceDTO.getProperties());
                            return srcds;
                        }).collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity())));

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

        result.setIntervalStartDateTime(
                renderJinja(
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime(),
                        result.getKeyBind()));

        result.setIntervalEndDateTime(
                renderJinja(
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime(),
                        result.getKeyBind()));
        return result;
    }

    private OffsetDateTime renderJinja(String template, Map<String,String> context){
        return  DateTimeUtils.parceDateTimeFormatWithTZ(jinjava.render(template, context));
    }
    private Set<DataSetDTO> collapseDataSetDTOs(
            DataSetDTO dataSetDTO,
            Map<String,DataSetDTO> sources){

        Set<DataSetDTO> result = new HashSet<>();
        result.addAll(sources.values());
        result.add(dataSetDTO);
        return result;
    }

    private Map<String,String> getKeyBind(
            ScheduledTaskLockDTO scheduledTaskLockDTO){
        Map<String,String> result = new HashMap<>();
        Map<String,String> entries = new HashMap<>();
        entries.putAll(System.getenv());
        entries.put(SystemVarKeys.TARGET_DATE_TIME_TZ_ISO_KEY, scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime());
        entries.forEach((key, value) -> result.put(String.format("${%s}", key), value));

        result.entrySet().forEach(stringEntry  ->
                logger.info("key -> {} | value -> {}", stringEntry.getKey(), LogPasswdRespectively.hidePasswords(stringEntry)));

        return result;
    }

    private Map<String, DataStoreDTO> getDataStores(DataSetDTO targetDataSet, Collection<DataSetDTO> sourceDatasets){
        Set<String> dataStoreNames = new HashSet<>();
        dataStoreNames.add(targetDataSet.getDataStoreKeyName());
        dataStoreNames.addAll( sourceDatasets
                        .stream()
                        .map(DataSetDTO::getDataStoreKeyName)
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
                                DataSetDTO::getKeyName,
                                dataSetDTO -> tableDefinitionFactory
                                        .buildTableDefinition(
                                                dataSetDTO,
                                                dataStoreDTOMap.get(
                                                        dataSetDTO.getDataStoreKeyName()))));
    }
}
