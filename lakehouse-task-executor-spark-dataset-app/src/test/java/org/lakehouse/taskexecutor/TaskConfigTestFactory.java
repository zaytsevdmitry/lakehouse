package org.lakehouse.taskexecutor;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.test.config.configuration.FileLoader;

import java.io.IOException;

public class TaskConfigTestFactory {


    public ScheduledTaskLockDTO loadScheduledTaskLockDTO(String dataSetKeyName, String taskName) throws IOException {

        ScheduledTaskLockDTO scheduledTaskLockDTO = new ScheduledTaskLockDTO();
        scheduledTaskLockDTO.setLockId(-1L);
        scheduledTaskLockDTO.setServiceId("test");
        scheduledTaskLockDTO.setLastHeartBeatDateTime(DateTimeUtils.nowStr());
        scheduledTaskLockDTO.setScheduledTaskEffectiveDTO(findScheduledTaskDTO(dataSetKeyName,taskName));
        return scheduledTaskLockDTO;
    }
    public static String targetDateTime = "2025-12-20T00:00:00Z";
    public static String intervalStart = targetDateTime;
    public static String intervalEnd = "2025-12-21T00:00:00Z";
    private ScheduledTaskDTO findScheduledTaskDTO(String dataSetKeyName, String taskName) throws IOException {
        FileLoader fileLoader = new FileLoader();
        ScheduledTaskDTO result = new ScheduledTaskDTO();

        TaskDTO t = fileLoader
                .loadScheduleEffectiveDTO()
                .getScenarioActs()
                .stream()
                .filter(a -> a.getDataSet().equals(dataSetKeyName))
                .flatMap(a-> a.getTasks().stream())
                .filter(taskDTO -> taskDTO.getName().equals(taskName))
                .toList().get(0);
        result.setDataSetKeyName(dataSetKeyName);
        result.setScheduleKeyName("unknown");
        result.setTaskProcessor(t.getTaskProcessor());
        result.setTaskProcessorBody(t.getTaskProcessorBody());
        result.setTaskProcessorArgs(t.getTaskProcessorArgs());
        result.setTargetDateTime(targetDateTime);
        result.setIntervalStartDateTime(intervalStart);
        result.setIntervalEndDateTime(intervalEnd);
        result.setId(1L);
        result.setScenarioActKeyName("unknown");
        result.setStatus(Status.Task.RUNNING);
        result.setTaskExecutionServiceGroupName("unknown");
        return result;
    }
}
