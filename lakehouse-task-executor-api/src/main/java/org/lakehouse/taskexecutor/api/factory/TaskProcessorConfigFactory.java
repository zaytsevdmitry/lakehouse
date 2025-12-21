package org.lakehouse.taskexecutor.api.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.LogPasswdRespectively;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
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

        Map<String, String> keyBind = new HashMap<>();

        keyBind.put(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime());
        keyBind.put(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime(), keyBind));
        keyBind.put(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY, jinjava.render(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime(), keyBind));

        result.setTargetDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY)));
        result.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_INTERVAL_START_TZ_KEY)));
        result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(keyBind.get(SystemVarKeys.TARGET_INTERVAL_END_TZ_KEY)));

        result.setLockSource(
                String.format("%s.%s.%s",
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime()));

        result.setTargetDataSetKeyName(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName());

        result.setExecutionModuleArgs(
                scheduledTaskLockDTO
                        .getScheduledTaskEffectiveDTO()
                        .getExecutionModuleArgs());


        result.setDataSetDTOs(collapseDataSetDTOs(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName()));
        result.setDataSources(
                result.getDataSetDTOs()
                        .values().stream()
                        .map(DataSetDTO::getDataSourceKeyName)
                        .collect(Collectors.toSet()).stream().map(configRestClientApi::getDataSourceDTO)
                        .collect(Collectors.toMap(DataSourceDTO::getKeyName, Function.identity()))
        );



        keyBind.putAll(configToContext(result));
        result.setKeyBind(keyBind);

        result.setScripts(
                result.getDataSetDTOs()
                        .get(result.getTargetDataSetKeyName())
                        .getScripts()
                        .stream()
                        .sorted(Comparator.comparingInt(DataSetScriptDTO::getOrder))
                        .map(DataSetScriptDTO::getKey)
                        .map(configRestClientApi::getScript)
                        .toList());

        logger.info("Config ready");
        try {
            logger.info(ObjectMapping.asJsonString(result));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private Map<String, String> configToContext(
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        Map<String, String> result = new HashMap<>();
        result.putAll(taskProcessorConfigDTO.getKeyBind());
        taskProcessorConfigDTO.getDataSetDTOs().values().forEach(dataSetDTO -> {
            result.put(SystemVarKeys.buildSourceTableFullName(dataSetDTO.getNameSpaceKeyName(), dataSetDTO.getKeyName()), dataSetDTO.getFullTableName());
            result.put(SystemVarKeys.buildSourceKeyName(dataSetDTO.getNameSpaceKeyName(), dataSetDTO.getKeyName()), dataSetDTO.getKeyName());
        });

        result.entrySet().forEach(stringEntry ->
                logger.info(
                        "key -> {} | value -> {}",
                        stringEntry.getKey(),
                        LogPasswdRespectively.hidePasswords(stringEntry)));
        return result;

    }

    private OffsetDateTime renderJinjaDateTime(String template, Map<String, String> context) {
        return DateTimeUtils.parseDateTimeFormatWithTZ(jinjava.render(template, context));
    }

    private Map<String,DataSetDTO> collapseDataSetDTOs(String targetDataSetKeyName) {
        Map<String,DataSetDTO> result = new HashMap<>();
        DataSetDTO targetDataSetDTO = configRestClientApi.getDataSetDTO(targetDataSetKeyName);
        Set<String> dataSetKeyNames = new HashSet<>();

        result.put(targetDataSetDTO.getKeyName(),targetDataSetDTO);
        dataSetKeyNames.addAll(
            targetDataSetDTO.getConstraints()
                    .stream()
                    .filter(c-> c.getType().equals(Types.Constraint.foreign))
                    .map(c -> c.getReference().getDataSetKeyName())
                    .toList()
                );
        dataSetKeyNames.addAll(
                targetDataSetDTO
                        .getSources()
                        .stream()
                        .map(DataSetSourceDTO::getDataSetKeyName)
                        .toList());
        result.putAll(dataSetKeyNames.stream()
                        .map(configRestClientApi::getDataSetDTO)
                        .collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity())));
        return result;
    }

}
