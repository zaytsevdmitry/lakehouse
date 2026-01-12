package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskResultDTO;
import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskConfigBuildException;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskProcessorConfigFactory;
import org.lakehouse.taskexecutor.factory.TaskProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final TaskProcessorFactory taskProcessorFactory;
    private final TaskProcessorConfigFactory taskProcessorConfigFactory;
    private final Map<Long, TaskProcessorConfigDTO> taskProcessorConfigDTOMap = new HashMap<>();

    private final HeardBeatService heardBeatService;

    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi,
            TaskProcessorFactory taskProcessorFactory,
            TaskProcessorConfigFactory taskProcessorConfigFactory,
            HeardBeatService heardBeatService) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.taskProcessorFactory = taskProcessorFactory;
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;
        this.heardBeatService = heardBeatService;
    }


    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO) throws TaskConfigBuildException {
        TaskProcessorConfigDTO taskProcessorConfigDTO = taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);
        taskProcessorConfigDTOMap.put(scheduledTaskLockDTO.getLockId(), taskProcessorConfigDTO);
        TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
        taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());

        TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
        taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());

        try {
            TaskProcessor p = taskProcessorFactory
                    .buildProcessor(
                            taskProcessorConfigDTO,
                            scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessor());
            heardBeatService.start(taskExecutionHeartBeatDTO);
            p.runTask();
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.SUCCESS));
            ;
        } catch (TaskConfigurationException e) {
            logger.error("Task creation error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.CONF_ERROR, e.toString()));
        } catch (TaskFailedException e) {
            logger.error("Task execution error {}", e.getMessage());
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } catch (RuntimeException e) {
            logger.error("Task execution error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } finally {
            logger.info("Status {}", taskInstanceReleaseDTO.getTaskResult().getStatus());
            heardBeatService.stop(taskExecutionHeartBeatDTO);
            logger.info("Heart beat shutdown");

            logger.info(
                    "Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}, status={}",
                    scheduledTaskLockDTO.getLockId(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                    taskInstanceReleaseDTO.getTaskResult().getStatus());

            schedulerRestClientApi.lockRelease(taskInstanceReleaseDTO);
        }
    }

    public TaskProcessorConfigDTO getTaskProcessorConfigDTO(Long lockId) {
        return taskProcessorConfigDTOMap.get(lockId);
    }

}
