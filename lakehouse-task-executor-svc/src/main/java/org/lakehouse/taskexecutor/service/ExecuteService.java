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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException.NotFound;

@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
  //  private final ConfigRestClientApi configRestClientApi;
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final String serviceId;
    private final String groupName;
    private final ProcessorFactory processorFactory;
    public ExecuteService(
           // ConfigRestClientApi configRestClientApi,
            SchedulerRestClientApi schedulerRestClientApi,
            @Value("${lakehouse.task-executor.service.id}") String serviceId,
            @Value("${lakehouse.task-executor.service.groupName}") String groupName,
            ProcessorFactory processorFactory) {
     //   this.configRestClientApi = configRestClientApi;
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.processorFactory = processorFactory;
        logger.info("Started executor with serviceId={} and groupName={}", serviceId, groupName);
    }

    public void takeAndRunTask(ScheduledTaskMsgDTO scheduledTaskMsgDTO, ScheduledTaskLockDTO scheduledTaskLockDTO) {

     /*   if (!scheduledTaskMsgDTO.getTaskExecutionServiceGroupName().equals(groupName)){
            logger.info(
                    "TaskId={} skipped because taskGroup {} not equals {}",
                    scheduledTaskMsgDTO.getId(),
                    scheduledTaskMsgDTO.getTaskExecutionServiceGroupName(),
                    groupName);
            return;
        }
*/
        try {
           /* ScheduledTaskLockDTO taskInstanceLockDTO = schedulerRestClientApi.lockTaskById(scheduledTaskMsgDTO.getId(), serviceId);

            logger.info("Lock lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}",
                    taskInstanceLockDTO.getLockId(),
                    taskInstanceLockDTO.getScheduledTaskEffectiveDTO().getName(),
                    taskInstanceLockDTO.getScheduleConfKeyName(),
                    taskInstanceLockDTO.getScheduleTargetDateTime(),
                    taskInstanceLockDTO.getScenarioActConfKeyName());
*/

            TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
            taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());

            TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
            taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());
            Thread hb = null;
            TaskLockHeartBeat taskLockHeartBeat = new TaskLockHeartBeat(schedulerRestClientApi, 10000, taskExecutionHeartBeatDTO);

            try {
                TaskProcessor p = processorFactory.buildProcessor(scheduledTaskLockDTO);
                hb = new Thread(taskLockHeartBeat);
                hb.start();
                p.run();
                taskInstanceReleaseDTO.setStatus(Status.Task.SUCCESS.label);
            }catch (Exception e ) {
                logger.error(e.getMessage());
                logger.error("Error when executing task processor", e);
                taskInstanceReleaseDTO.setStatus(Status.Task.FAILED.label);
            } finally {
                taskLockHeartBeat.setExit();
                if (hb != null && hb.isAlive() && !hb.isInterrupted())
                    hb.interrupt();
                logger.info("Heart beat shutdown");


                logger.info(
                        "Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}",
                        scheduledTaskLockDTO.getLockId(),
                        scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                        scheduledTaskLockDTO.getScheduleConfKeyName(),
                        scheduledTaskLockDTO.getScheduleTargetDateTime(),
                        scheduledTaskLockDTO.getScenarioActConfKeyName());

                schedulerRestClientApi.lockRelease(taskInstanceReleaseDTO);

            }
        } catch (NotFound | ResourceAccessException e) {
            logger.warn(e.getMessage());
        }
    }

}
