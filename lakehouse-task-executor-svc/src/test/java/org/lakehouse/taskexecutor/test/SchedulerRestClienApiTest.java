package org.lakehouse.taskexecutor.test;

import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;

import java.util.List;

public class SchedulerRestClienApiTest implements SchedulerRestClientApi{
    @Override
    public ScheduledTaskMsgDTO getScheduledTaskDTO(String name) {
        return null;
    }

    @Override
    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id) {
        return null;
    }

    @Override
    public int deleteScheduledTaskDTO(String name) {
        return 0;
    }

    @Override
    public int postScheduledTaskDTO(ScheduledTaskMsgDTO o) {
        return 0;
    }

    @Override
    public List<ScheduledTaskDTO> getScheduledTaskDTOList() {
        return List.of();
    }

    @Override
    public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOList() {
        return List.of();
    }

    @Override
    public ScheduledTaskLockDTO lockTaskById(Long taskId, String serviceId) {
        return null;
    }

    @Override
    public int lockHeartBeat(TaskExecutionHeartBeatDTO taskExecutionHeartBeat) {
        return 0;
    }

    @Override
    public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
        return 0;
    }
}
