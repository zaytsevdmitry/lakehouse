package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.exception.TaskStatusException;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final StateRestClientApi stateRestClientApi;
    private final String serviceId;
    private final String groupName;
    private final long heartBeatIntervalMs;
    private final ProcessorFactory processorFactory;
    private final TaskProcessorConfigFactory taskProcessorConfigFactory;
    private final Jinjava jinjava;
    private final HeardBeatService heardBeatService;
    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi,
            StateRestClientApi stateRestClientApi,
            @Value("${lakehouse.task-executor.service.id}") String serviceId,
            @Value("${lakehouse.task-executor.service.groupName}") String groupName,
            @Value("${lakehouse.task-executor.service.heart-beat-interval-ms}") long heartBeatIntervalMs,
            ProcessorFactory processorFactory, TaskProcessorConfigFactory taskProcessorConfigFactory, Jinjava jinjava, HeardBeatService heardBeatService) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.stateRestClientApi = stateRestClientApi;
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
        this.processorFactory = processorFactory;
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;
        this.jinjava = jinjava;
        this.heardBeatService = heardBeatService;
        logger.info("Started executor with serviceId={} and groupName={}", serviceId, groupName);
    }


    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO)  {
        TaskProcessorConfig taskProcessorConfig = taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);
        DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.RUNNING, taskProcessorConfig);

        stateRestClientApi
                .setDataSetStateDTO(dataSetStateDTO);

        TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
        taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());

        TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
        taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());
        //Thread hb = null;
        //TaskLockHeartBeat taskLockHeartBeat = new TaskLockHeartBeat(schedulerRestClientApi, heartBeatIntervalMs, taskExecutionHeartBeatDTO);
        try {
            TaskProcessor p = processorFactory.buildProcessor(taskProcessorConfig,scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getExecutionModule());
          /*  hb = new Thread(taskLockHeartBeat);
            hb.start();*/
            heardBeatService.start(taskExecutionHeartBeatDTO);
            String status = p.runTask().label;
            taskInstanceReleaseDTO.setStatus(status);
        } catch (ReflectiveOperationException   e){
            logger.error("Task execution error ", e);
            taskInstanceReleaseDTO.setStatus(Status.Task.CONF_ERROR.label);
        } finally {
            logger.info("Status {}", taskInstanceReleaseDTO.getStatus());
            //taskLockHeartBeat.setExit();
/*            if (hb != null && hb.isAlive() && !hb.isInterrupted())
                hb.interrupt();*/
            heardBeatService.stop();
            logger.info("Heart beat shutdown");


            logger.info(
                    "Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}, status={}",
                    scheduledTaskLockDTO.getLockId(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                    taskInstanceReleaseDTO.getStatus());

            schedulerRestClientApi.lockRelease(taskInstanceReleaseDTO);


        }
        if (taskInstanceReleaseDTO.getStatus().equals(Status.Task.FAILED.label)) {
            logger.info("Send dataset {} status {} ",
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName(),
                    Status.Task.FAILED.label);
            dataSetStateDTO.setStatus(Status.DataSet.FAILED.label);
            stateRestClientApi
                    .setDataSetStateDTO(dataSetStateDTO);
        }

    }

}
