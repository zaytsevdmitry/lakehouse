package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final ProcessorFactory processorFactory;
    private final TaskProcessorConfigFactory taskProcessorConfigFactory;
    private final HeardBeatService heardBeatService;
    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi,
            ProcessorFactory processorFactory,
            TaskProcessorConfigFactory taskProcessorConfigFactory,
            HeardBeatService heardBeatService) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.processorFactory = processorFactory;
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;
        this.heardBeatService = heardBeatService;
    }


    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO)  {
        TaskProcessorConfig taskProcessorConfig = taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);

        /*DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.RUNNING, taskProcessorConfig);
*/
        /*stateRestClientApi
                .setDataSetStateDTO(dataSetStateDTO);
*/
        TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
        taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());

        TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
        taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());

        try {
            TaskProcessor p = processorFactory
                    .buildProcessor(
                            taskProcessorConfig,
                            scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getExecutionModule());
            heardBeatService.start(taskExecutionHeartBeatDTO);
            String status = p.runTask().label;
            taskInstanceReleaseDTO.setStatus(status);
        } catch (ReflectiveOperationException   e){
            logger.error("Task execution error ", e);
            taskInstanceReleaseDTO.setStatus(Status.Task.CONF_ERROR.label);
        } finally {
            logger.info("Status {}", taskInstanceReleaseDTO.getStatus());
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
        /*if (taskInstanceReleaseDTO.getStatus().equals(Status.Task.FAILED.label)) {
            logger.info("Send dataset {} status {} Target",
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName(),
                    Status.Task.FAILED.label);
            dataSetStateDTO.setStatus(Status.DataSet.FAILED.label);
            logger.info("State {}", dataSetStateDTO);
            stateRestClientApi
                    .setDataSetStateDTO(dataSetStateDTO);
        }*/

    }

}
