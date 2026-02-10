package org.lakehouse.taskexecutor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskResultDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;


@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final ConfigRestClientApi configRestClientApi;
    private final ConfigurableApplicationContext applicationContext;
    private final HeardBeatService heardBeatService;
    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi, ConfigRestClientApi configRestClientApi,
            ConfigurableApplicationContext applicationContext,
            HeardBeatService heardBeatService) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.configRestClientApi = configRestClientApi;
        this.applicationContext = applicationContext;
        this.heardBeatService = heardBeatService;
    }


    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO)  {
        TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
        taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());
        TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
        taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());

        try {
            TaskProcessor p = (TaskProcessor) applicationContext.getBean(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessor());
            SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName());
            // made task globalContext based on task and source information
            JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO()));

            heardBeatService.start(taskExecutionHeartBeatDTO);

            p.runTask(sourceConfDTO,scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), jinJavaUtils);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.SUCCESS));
        } catch (TaskConfigurationException e) {
            logger.error("Task creation error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.CONF_ERROR, e.toString()));
        } catch (TaskFailedException e) {
            logger.error("Task execution error {}", e.getMessage());
            logger.error(e.getMessage(),e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } catch (RuntimeException e) {
            logger.error("Task execution error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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



}
