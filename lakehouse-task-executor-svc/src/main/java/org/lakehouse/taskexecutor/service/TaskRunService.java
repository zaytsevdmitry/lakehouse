package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.rest.config.ConfigRestClientApiImpl;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.executionmodule.ProcessorFactory;
import org.lakehouse.taskexecutor.executionmodule.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.ResourceAccessException;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;

@Service
@EnableConfigurationProperties
@ComponentScan(basePackages = { "org.lakehouse.api.rest.client.service",
		"org.lakehouse.api.rest.client.configuration" })
public class TaskRunService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final SchedulerRestClientApi clientApi;
	private final ProcessorFactory processorFactory;
	private final String serviceId;
	private final String groupName;
	//private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public TaskRunService(SchedulerRestClientApi clientApi, ProcessorFactory processorFactory,
                          @Value("${lakehouse.taskexecutor.service.id}") String serviceId,
                          @Value("${lakehouse.taskexecutor.service.groupName}") String groupName //,
                          //ThreadPoolTaskExecutor threadPoolTaskExecutor
			) {
		this.clientApi = clientApi;
		this.processorFactory = processorFactory;
		this.serviceId = serviceId;
		this.groupName = groupName;
		//this.threadPoolTaskExecutor = threadPoolTaskExecutor;

	}

	@Scheduled(fixedDelayString = "${lakehouse.taskexecutor.schedule.lockTask.delay-ms}", initialDelayString = "${lakehouse.taskexecutor.schedule.lockTask.initial-delay-ms}")

	public void takeAndRunTask() {

		try {
			ScheduledTaskLockDTO taskInstanceLockDTO = clientApi.lockTask(groupName, serviceId);

			logger.info("Lock lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}",
					taskInstanceLockDTO.getLockId(), taskInstanceLockDTO.getScheduledTaskDTO().getName(),
					taskInstanceLockDTO.getScheduledTaskDTO().getScheduleName(),
					taskInstanceLockDTO.getScheduledTaskDTO().getScheduleTargetTimestamp(),
					taskInstanceLockDTO.getScheduledTaskDTO().getScenarioActName());

			TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
			taskInstanceReleaseDTO.setLockId(taskInstanceLockDTO.getLockId());

			TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
			taskExecutionHeartBeatDTO.setLockId(taskInstanceLockDTO.getLockId());
			Thread hb = null;
			
			try {
				TaskProcessor p = processorFactory.buildProcessor(taskInstanceLockDTO.getScheduledTaskDTO());
				hb = new Thread(new TaskLockHeartBeat(clientApi, 10000, taskExecutionHeartBeatDTO));
				hb.start();
				p.run();
				taskInstanceReleaseDTO.setStatus(Status.Task.SUCCESS.label);
			}catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
				taskInstanceReleaseDTO.setStatus(Status.Task.FAILED.label);
			} finally {
				if (hb != null && hb.isAlive() && !hb.isInterrupted())
					hb.interrupt();
				logger.info("Heart beat shutdown");
				
				clientApi.lockRelease(taskInstanceReleaseDTO);

				logger.info(
						"Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}",
						taskInstanceLockDTO.getLockId(), taskInstanceLockDTO.getScheduledTaskDTO().getName(),
						taskInstanceLockDTO.getScheduledTaskDTO().getScheduleName(),
						taskInstanceLockDTO.getScheduledTaskDTO().getScheduleTargetTimestamp(),
						taskInstanceLockDTO.getScheduledTaskDTO().getScenarioActName());				
			}
		} catch (NotFound | ResourceAccessException e) {
			logger.warn(e.getMessage());
		}
	}
}
