package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleScenarioActEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.service.ScheduleEffectiveService;
import org.springframework.stereotype.Component;

@Component
public class ScheduleTaskInstanceFactory {
    private final ConfigRestClientApi configRestClientApi;
    private final ScheduleEffectiveService scheduleEffectiveService;

    public ScheduleTaskInstanceFactory(
            ConfigRestClientApi configRestClientApi,
            ScheduleEffectiveService scheduleEffectiveService) {
        this.configRestClientApi = configRestClientApi;
        this.scheduleEffectiveService = scheduleEffectiveService;
    }

    public static ScheduleTaskInstance mapToNewScheduleTaskInstance(
            TaskDTO taskDTO,
            ScheduleScenarioActInstance scheduleScenarioActInstance) {
        ScheduleTaskInstance result = new ScheduleTaskInstance();
        result.setName(taskDTO.getName());
        result.setScheduleScenarioActInstance(scheduleScenarioActInstance);
        result.setStatus(Status.Task.NEW.label);
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

        result.setIntervalStartDateTime(actDTO.getIntervalStart());
        result.setIntervalEndDateTime(actDTO.getIntervalEnd());

        result.setTargetDateTime(
                DateTimeUtils
                        .formatDateTimeFormatWithTZ(sti
                                .getScheduleScenarioActInstance()
                                .getScheduleInstance()
                                .getTargetExecutionDateTime()));

        result.setDataSetKeyName(actDTO.getDataSet());
        return result;
    }
}
