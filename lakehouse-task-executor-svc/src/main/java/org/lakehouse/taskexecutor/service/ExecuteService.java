package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final StateRestClientApi stateRestClientApi;
    private final String serviceId;
    private final String groupName;
    private final long heartBeatIntervalMs;
    private final ProcessorFactory processorFactory;
    private final Jinjava jinjava;
    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi,
            StateRestClientApi stateRestClientApi,
            @Value("${lakehouse.task-executor.service.id}") String serviceId,
            @Value("${lakehouse.task-executor.service.groupName}") String groupName,
            @Value("${lakehouse.task-executor.service.heart-beat-interval-ms}") long heartBeatIntervalMs,
            ProcessorFactory processorFactory, Jinjava jinjava) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.stateRestClientApi = stateRestClientApi;
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
        this.processorFactory = processorFactory;
        this.jinjava = jinjava;
        logger.info("Started executor with serviceId={} and groupName={}", serviceId, groupName);
    }

    private DataSetStateDTO getDataSetStateDTO(Status.DataSet status, ScheduledTaskDTO scheduledTaskDTO) {
        Map<String,String> map = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, scheduledTaskDTO.getTargetDateTime()));
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(scheduledTaskDTO.getDataSetKeyName());
        result.setIntervalStartDateTime(jinjava.render(scheduledTaskDTO.getIntervalStartDateTime(),map));
        result.setIntervalEndDateTime(jinjava.render(scheduledTaskDTO.getIntervalEndDateTime(),map));
        result.setStatus(status.label);

        return result;
    }

    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO) {

        stateRestClientApi
                .setDataSetStateDTO(
                        getDataSetStateDTO(
                                Status.DataSet.RUNNING,
                                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO()));

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
            logger.info("Status {}", taskInstanceReleaseDTO.getStatus());
            taskLockHeartBeat.setExit();
            if (hb != null && hb.isAlive() && !hb.isInterrupted())
                hb.interrupt();
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
        if (taskInstanceReleaseDTO.getStatus().equals(Status.Task.FAILED)) {
            logger.info("Send dataset {} status {} ",
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName(),
                    Status.Task.FAILED.label);
            stateRestClientApi.setDataSetStateDTO(getDataSetStateDTO(Status.DataSet.FAILED, scheduledTaskLockDTO.getScheduledTaskEffectiveDTO()));
        }
        //  } catch (NotFound | ResourceAccessException e) {
        //     logger.warn(e.getMessage());
        //  }
    }

}
