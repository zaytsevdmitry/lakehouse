package org.lakehouse.taskexecutor.api.factory.taskconf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskProcessorConfigFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConfigRestClientApi configRestClientApi;
    private final Jinjava jinjava;

    public TaskProcessorConfigFactory(
            ConfigRestClientApi configRestClientApi,
            Jinjava jinjava) {
        this.configRestClientApi = configRestClientApi;
        this.jinjava = jinjava;
    }
    private void validateScheduledTaskLockDTO(ScheduledTaskLockDTO scheduledTaskLockDTO) throws TaskConfigBuildException{
        if (scheduledTaskLockDTO == null) throw new TaskConfigBuildException("scheduledTaskLockDTO can't be null");
        if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO() == null) throw new TaskConfigBuildException("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO can't be null");
        if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime() == null) throw new TaskConfigBuildException("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO.getIntervalStartDateTime can't be null");
        if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime() == null) throw new TaskConfigBuildException("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO.getIntervalEndDateTime can't be null");

    }
    public TaskProcessorConfigDTO buildTaskProcessorConfig(ScheduledTaskLockDTO scheduledTaskLockDTO) throws TaskConfigBuildException {
        validateScheduledTaskLockDTO(scheduledTaskLockDTO);
        logger.info("Build config");


        TaskProcessorConfigDTO result = new TaskProcessorConfigDTO();
        Map<String, Object> factoryContext = new HashMap<>();

        factoryContext.put(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY,scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime() );
        factoryContext.put(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime(), factoryContext));
        factoryContext.put(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime(), factoryContext));

        result.setTargetDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime()));
        result.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ((String) factoryContext.get(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY)));
        result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ((String) factoryContext.get(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY)));
        result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ((String) factoryContext.get(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY)));

        result.setLockSource(
                String.format("%s.%s.%s",
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime()));

        result.setTaskFullName(
                String.format("%s.%s.%s.%s",
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime()));

        result.setTargetDataSetKeyName(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName());
        result.setTaskProcessorBody(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessorBody());
        result.setTaskProcessorArgs(
                scheduledTaskLockDTO
                        .getScheduledTaskEffectiveDTO()
                        .getTaskProcessorArgs());



        result.setDataSets(collapseDataSetDTOs(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName()));
        result.setDataSources(
                result.getDataSets()
                        .values().stream()
                        .map(DataSetDTO::getDataSourceKeyName)
                        .collect(Collectors.toSet()).stream().map(configRestClientApi::getDataSourceDTO)
                        .collect(Collectors.toMap(DataSourceDTO::getKeyName, Function.identity()))
        );
        result.setDrivers(
            result.getDataSources()
                    .values()
                    .stream()
                    .map(DataSourceDTO::getDriverKeyName)
                    .collect(Collectors.toSet())
                    .stream()
                    .map(configRestClientApi::getDriverDTO)
                    .collect(Collectors.toMap(DriverDTO::getKeyName,Function.identity()))
        );


        result.setScripts(
                result.getDataSets()
                        .get(result.getTargetDataSetKeyName())
                        .getScripts()
                        .stream()
                        .sorted(Comparator.comparingInt(DataSetScriptDTO::getOrder))
                        .map(DataSetScriptDTO::getKey)
                        .collect(Collectors.toMap(Function.identity(), configRestClientApi::getScript)));

        resolveTemplates(result);


        logger.info("Config ready");
        try {
            logger.info(ObjectMapping.asJsonString(result));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }



    private void resolveTemplates(
            TaskProcessorConfigDTO taskProcessorConfigDTO){
        taskProcessorConfigDTO.getDataSources().forEach((s, dataSourceDTO) ->
        {
            Map<String,Object> localContext = new HashMap<>();
            localContext.put(SystemVarKeys.DRIVER_KEY, taskProcessorConfigDTO.getDrivers().get(dataSourceDTO.getDriverKeyName()));
            localContext.put(SystemVarKeys.DATASOURCE_KEY, dataSourceDTO);
            localContext.put(SystemVarKeys.SERVICE_KEY, dataSourceDTO.getService());

            dataSourceDTO.getService().setProperties(rerenderMap(dataSourceDTO.getService().getProperties(), localContext));

        });
    }

    private Map<String,String> rerenderMap(Map<String,String> map, Map<String,Object> localContext){
        return map.entrySet().stream().map(e->
                Map.entry(
                        jinjava.render(e.getKey(), localContext),
                        jinjava.render(e.getValue(),localContext))

        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String,DataSetDTO> collapseDataSetDTOs(String targetDataSetKeyName) {
        Map<String,DataSetDTO> result = new HashMap<>();
        DataSetDTO targetDataSetDTO = configRestClientApi.getDataSetDTO(targetDataSetKeyName);
        Set<String> dataSetKeyNames = new HashSet<>();

        result.put(targetDataSetDTO.getKeyName(),targetDataSetDTO);
        dataSetKeyNames.addAll(
            targetDataSetDTO.getConstraints()
                    .values()
                    .stream()
                    .filter(c-> c.getType().equals(Types.Constraint.foreign))
                    .map(c -> c.getReference().getDataSetKeyName())
                    .toList()
                );
        dataSetKeyNames.addAll(
                targetDataSetDTO
                        .getSources().keySet());
        result.putAll(dataSetKeyNames.stream()
                        .map(configRestClientApi::getDataSetDTO)
                        .collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity())));
        return result;
    }


}
