package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.LogPasswdRespectively;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.common.api.task.processor.entity.TableDefinition;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
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
            TableDefinitionFactory tableDefinitionFactory,
            Jinjava jinjava) {
        this.configRestClientApi = configRestClientApi;
        this.tableDefinitionFactory = tableDefinitionFactory;
        this.jinjava = jinjava;
    }

    public TaskProcessorConfigDTO buildTaskProcessorConfig(ScheduledTaskLockDTO scheduledTaskLockDTO){
        logger.info("Build config");

        TaskProcessorConfigDTO result = new TaskProcessorConfigDTO();

        Map<String,String> keyBind = new HashMap<>();

        keyBind.put(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime());
        keyBind.put(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime(),keyBind));
        keyBind.put(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime(),keyBind));

        result.setTargetDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY)));
        result.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY)));
        result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY)));

        result.setLockSource(
                String.format("%s.%s.%s",
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime()));

        result.setTargetDataSet(
                configRestClientApi.getDataSetDTO(
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName()));

        result.setExecutionModuleArgs(
                scheduledTaskLockDTO
                        .getScheduledTaskEffectiveDTO()
                        .getExecutionModuleArgs());

        result.setSources(
                result.getTargetDataSet().getSources().stream()
                        .map(dataSetSourceDTO -> {
                            DataSetDTO srcds = configRestClientApi.getDataSetDTO(dataSetSourceDTO.getName());
                            srcds.getProperties().putAll(dataSetSourceDTO.getProperties());
                            return srcds;
                        }).collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity())));

        result.setDataStores(getDataStores(result.getTargetDataSet(),result.getSources().values()));

        Set<DataSetDTO> dataSets = collapseDataSetDTOs(result.getTargetDataSet(),result.getSources());

        result.setDataSetDTOSet(dataSets);


        result.setTableDefinitions(getTableDefinitionMap(dataSets,result.getDataStores()));

        keyBind.putAll(configToContext(result));
        result.setKeyBind(keyBind);

        result.setScripts(result.getTargetDataSet()
                .getScripts()
                .stream()
                .sorted(Comparator.comparingInt(DataSetScriptDTO::getOrder ))
                .map(DataSetScriptDTO::getKey)
                .map(configRestClientApi::getScript)
                .map(script-> jinjava.render(script, result.getKeyBind()))
                .toList());



        logger.info("Config ready");
        return result;
    }

    private Map<String,String> configToContext(
            TaskProcessorConfigDTO taskProcessorConfigDTO){
        Map<String,String> result = new HashMap<>();
        result.putAll(taskProcessorConfigDTO.getKeyBind());
        taskProcessorConfigDTO.getDataSetDTOSet().forEach(dataSetDTO -> {
            result.put(SystemVarKeys.buildSourceTableFullName(dataSetDTO.getProject(),dataSetDTO.getKeyName()), dataSetDTO.getFullTableName());
            result.put(SystemVarKeys.buildSourceKeyName(dataSetDTO.getProject(),dataSetDTO.getKeyName()), dataSetDTO.getKeyName());
        });

        result.entrySet().forEach(stringEntry  ->
                logger.info(
                        "key -> {} | value -> {}",
                        stringEntry.getKey(),
                        LogPasswdRespectively.hidePasswords(stringEntry)));
        return result;

    }

    private OffsetDateTime renderJinjaDateTime(String template, Map<String,String> context){
        return  DateTimeUtils.parseDateTimeFormatWithTZ(jinjava.render(template, context));
    }
    private Set<DataSetDTO> collapseDataSetDTOs(
            DataSetDTO dataSetDTO,
            Map<String,DataSetDTO> sources){

        Set<DataSetDTO> result = new HashSet<>();
        result.addAll(sources.values());
        result.add(dataSetDTO);
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
