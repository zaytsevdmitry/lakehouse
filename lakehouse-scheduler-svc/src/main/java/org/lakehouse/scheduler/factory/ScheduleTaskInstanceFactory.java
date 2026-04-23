package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleScenarioActEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.service.ScheduleEffectiveService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleTaskInstanceFactory {
    private final ConfigRestClientApi configRestClientApi;
    private final ScheduleEffectiveService scheduleEffectiveService;
    private final JinJavaUtils jinJavaUtils;
    public ScheduleTaskInstanceFactory(
            ConfigRestClientApi configRestClientApi,
            ScheduleEffectiveService scheduleEffectiveService, JinJavaUtils jinJavaUtils) {
        this.configRestClientApi = configRestClientApi;
        this.scheduleEffectiveService = scheduleEffectiveService;
        this.jinJavaUtils = jinJavaUtils;
    }

    public static ScheduleTaskInstance mapToNewScheduleTaskInstance(
            TaskDTO taskDTO,
            ScheduleScenarioActInstance scheduleScenarioActInstance) {
        ScheduleTaskInstance result = new ScheduleTaskInstance();
        result.setName(taskDTO.getName());
        result.setScheduleScenarioActInstance(scheduleScenarioActInstance);
        result.setStatus(Status.Task.NEW);
        return result;
    }

    public ScheduledTaskDTO mapScheduledTaskToDTO(ScheduleTaskInstance sti) {
        ScheduledTaskDTO result = new ScheduledTaskDTO();
        result.setId(sti.getId());
        result.setScenarioActKeyName(sti.getScheduleScenarioActInstance().getName());
        result.setScheduleKeyName(sti.getScheduleScenarioActInstance().getScheduleInstance().getConfigScheduleKeyName());

        TaskDTO taskDTO = configRestClientApi.getEffectiveTaskDTO(
                sti.getScheduleScenarioActInstance().getScheduleInstance().getConfigScheduleKeyName(),
                sti.getScheduleScenarioActInstance().getName(),
                sti.getName());

        result.setTaskProcessor(taskDTO.getTaskProcessor());
        result.setTaskProcessorBody(taskDTO.getTaskProcessorBody());
        result.setTaskProcessorArgs(taskDTO.getTaskProcessorArgs());
        result.setName(sti.getName());
        result.setStatus(sti.getStatus());
        result.setTaskExecutionServiceGroupName(taskDTO.getTaskExecutionServiceGroupName());

        ScheduleScenarioActEffectiveDTO actDTO = scheduleEffectiveService
                .getScheduleEffectiveDTO(
                        sti.getScheduleScenarioActInstance()
                                .getScheduleInstance()
                                .getConfigScheduleKeyName())
                .getScenarioActs()
                .stream().filter(scheduleScenarioActEffectiveDTO -> scheduleScenarioActEffectiveDTO.getName()
                        .equals(sti.getScheduleScenarioActInstance().getName())).toList().get(0);

        String targetEDTStr = DateTimeUtils
                .formatDateTimeFormatWithTZ(sti
                        .getScheduleScenarioActInstance()
                        .getScheduleInstance()
                        .getTargetExecutionDateTime());

        Map<String, Object> localContext = new HashMap<>();
        localContext.put(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetEDTStr);

        result.setIntervalStartDateTime(jinJavaUtils.render(actDTO.getIntervalStart(),localContext));
        result.setIntervalEndDateTime(jinJavaUtils.render(actDTO.getIntervalEnd(),localContext));
        result.setTargetDateTime(targetEDTStr);
        result.setDataSetKeyName(actDTO.getDataSet());
        return result;
    }
}
