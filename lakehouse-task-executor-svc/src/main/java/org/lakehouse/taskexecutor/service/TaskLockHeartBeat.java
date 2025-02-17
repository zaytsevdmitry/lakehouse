package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.rest.config.ConfigRestClientApiImpl;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;

public class TaskLockHeartBeat implements Runnable{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final SchedulerRestClientApi clientApi;
	private final TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO;
	private final Integer heartBeatIntervalMs;
	private boolean exit;
	
	public TaskLockHeartBeat(
			SchedulerRestClientApi clientApi,
			Integer heartBeatIntervalMs,
			TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO ) {
		this.clientApi = clientApi;
		this.heartBeatIntervalMs = heartBeatIntervalMs;
		this.taskExecutionHeartBeatDTO = taskExecutionHeartBeatDTO;
	}
	public void setExit(){
		exit = true;
	}

	@Override
	public void run() {
		while(!exit) {
			try {
				Thread.sleep(heartBeatIntervalMs);
				clientApi.lockHeartBeat(taskExecutionHeartBeatDTO);
				logger.info("Heart beat lockid={} sended", taskExecutionHeartBeatDTO.getLockId());
			} catch (NotFound | InterruptedException e) { 
				logger.warn(e.getMessage());
				exit = true;
			}
		}
	}	

}
