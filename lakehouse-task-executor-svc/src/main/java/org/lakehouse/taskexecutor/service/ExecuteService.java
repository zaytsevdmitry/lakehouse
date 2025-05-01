package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;

import java.lang.reflect.InvocationTargetException;

@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final String serviceId;
    private final String groupName;
    private final long heartBeatIntervalMs;
    private final ProcessorFactory processorFactory;
    public ExecuteService(
           // ConfigRestClientApi configRestClientApi,
            SchedulerRestClientApi schedulerRestClientApi,
            @Value("${lakehouse.task-executor.service.id}") String serviceId,
            @Value("${lakehouse.task-executor.service.groupName}") String groupName,
            @Value("${lakehouse.task-executor.service.heart-beat-interval-ms}") long heartBeatIntervalMs,
            ProcessorFactory processorFactory) {
     //   this.configRestClientApi = configRestClientApi;
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
        this.processorFactory = processorFactory;
        logger.info("Started executor with serviceId={} and groupName={}", serviceId, groupName);
    }

    public void takeAndRunTask(ScheduledTaskMsgDTO scheduledTaskMsgDTO, ScheduledTaskLockDTO scheduledTaskLockDTO) {

            TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
            taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());

            TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
            taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());
            Thread hb = null;
            TaskLockHeartBeat taskLockHeartBeat = new TaskLockHeartBeat(schedulerRestClientApi, heartBeatIntervalMs, taskExecutionHeartBeatDTO);

            try {
                TaskProcessor p = processorFactory.buildProcessor(scheduledTaskLockDTO);
                hb = new Thread(taskLockHeartBeat);
                hb.start();
                taskInstanceReleaseDTO.setStatus(p.runTask().label);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                logger.error("Task execution error ", e);
                taskInstanceReleaseDTO.setStatus(Status.Task.FAILED.label);
            } finally {
                logger.info("Status {}",taskInstanceReleaseDTO.getStatus());
                taskLockHeartBeat.setExit();
                if (hb != null && hb.isAlive() && !hb.isInterrupted())
                    hb.interrupt();
                logger.info("Heart beat shutdown");


                logger.info(
                        "Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}, status={}",
                        scheduledTaskLockDTO.getLockId(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                        scheduledTaskLockDTO.getScheduleConfKeyName(),
                        scheduledTaskLockDTO.getScheduleTargetDateTime(),
                        scheduledTaskLockDTO.getScenarioActConfKeyName(),
                        taskInstanceReleaseDTO.getStatus());

                schedulerRestClientApi.lockRelease(taskInstanceReleaseDTO);

            }
      //  } catch (NotFound | ResourceAccessException e) {
       //     logger.warn(e.getMessage());
      //  }
    }

}
