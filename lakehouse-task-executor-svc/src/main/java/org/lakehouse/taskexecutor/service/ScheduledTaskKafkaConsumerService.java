package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.exception.TaskLockTryExcessedException;
import org.lakehouse.taskexecutor.exception.TaskLockNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class ScheduledTaskKafkaConsumerService  {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExecuteService executeService;
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final String serviceId;
    private final String groupName;
    private final Integer maxLockRetries;
    private final Integer maxLockRetriesDuration;

    public ScheduledTaskKafkaConsumerService(
            ExecuteService executeService,
            SchedulerRestClientApi schedulerRestClientApi,
            @Value("${lakehouse.task-executor.service.id}") String serviceId,
            @Value("${lakehouse.task-executor.service.groupName}") String groupName,
            @Value("${lakehouse.task-executor.service.max-lock-retries}") Integer maxLockRetries,
            @Value("${lakehouse.task-executor.service.max-lock-retries-duration-ms}") Integer maxLockRetriesDuration
            ){
        this.executeService = executeService;
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.maxLockRetries = maxLockRetries;
        this.maxLockRetriesDuration = maxLockRetriesDuration;
    }

    @KafkaListener(
            topics =      "#{'${lakehouse.task-executor.scheduled.task.kafka.consumer.topics}'.split(',')}",
            concurrency = "#{'${lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency}'}",
            containerFactory = "containerFactory")
    public void listen(ScheduledTaskMsgDTO scheduledTaskMsgDTO, Acknowledgment acknowledgment) throws Exception {


        logger.info("New task: id={} taskGroup {}",
                scheduledTaskMsgDTO.getId(),
                scheduledTaskMsgDTO.getTaskExecutionServiceGroupName());

        ScheduledTaskLockDTO taskInstanceLockDTO = null;

        if (scheduledTaskMsgDTO.getTaskExecutionServiceGroupName().equals(groupName)){
                taskInstanceLockDTO = takeLockWithReTries(scheduledTaskMsgDTO,0);
        }else {
            logger.info(
                    "TaskId={} skipped because taskGroup {} not equals {}",
                    scheduledTaskMsgDTO.getId(),
                    scheduledTaskMsgDTO.getTaskExecutionServiceGroupName(),
                    groupName);
        }
        acknowledgment.acknowledge();

        if (taskInstanceLockDTO != null)
            executeService.takeAndRunTask(scheduledTaskMsgDTO, taskInstanceLockDTO);

        logger.info("Iteration is done");
    }


    private ScheduledTaskLockDTO takeLockWithReTries(ScheduledTaskMsgDTO scheduledTaskMsgDTO, int tryNum)
            throws TaskLockTryExcessedException, InterruptedException {

        if (tryNum == maxLockRetries) {
            throw new TaskLockTryExcessedException(tryNum);
        }

        ScheduledTaskLockDTO taskInstanceLockDTO = null;

        try {//todo Caused by: java.net.ConnectException: Connection refused

            logger.info("Trying to lock task id={}", scheduledTaskMsgDTO.getId());

            taskInstanceLockDTO = schedulerRestClientApi.lockTaskById(scheduledTaskMsgDTO.getId(), serviceId);

            logger.info("Lock taken lockId={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}",
                        taskInstanceLockDTO.getLockId(),
                        taskInstanceLockDTO.getScheduledTaskEffectiveDTO().getName(),
                        taskInstanceLockDTO.getScheduleConfKeyName(),
                        taskInstanceLockDTO.getScheduleTargetDateTime(),
                        taskInstanceLockDTO.getScenarioActConfKeyName());


        } catch (HttpClientErrorException.NotFound nfe) {
                logger.info("Already resolved {}", nfe.getMessage());
        } catch (HttpServerErrorException e) {
            logger.warn("can't get the lock for  task: id={} taskGroup {}, tryNum {}",
                    scheduledTaskMsgDTO.getId(),
                    scheduledTaskMsgDTO.getTaskExecutionServiceGroupName(),
                    tryNum);

            logger.error(e.fillInStackTrace().toString());
            logger.info("Waiting {} ms",maxLockRetriesDuration);
            Thread.sleep(maxLockRetriesDuration);
            tryNum++;
            logger.info("Retry {} to lock lockId={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={} ",
                    tryNum,
                    taskInstanceLockDTO.getLockId(),
                    taskInstanceLockDTO.getScheduledTaskEffectiveDTO().getName(),
                    taskInstanceLockDTO.getScheduleConfKeyName(),
                    taskInstanceLockDTO.getScheduleTargetDateTime(),
                    taskInstanceLockDTO.getScenarioActConfKeyName());
            taskInstanceLockDTO = takeLockWithReTries(scheduledTaskMsgDTO, tryNum);
        }
        return taskInstanceLockDTO;
    }
}
