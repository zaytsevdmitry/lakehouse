package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class HeardBeatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private  TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO;
    private final SchedulerRestClientApi schedulerRestClientApi;

    public HeardBeatService(SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
    }

    @Scheduled(
            fixedDelayString = "${lakehouse.task-executor.service.heart-beat-interval-ms}",
            initialDelayString = "${lakehouse.task-executor.service.heart-beat-initial-delaY-ms}")
    public void sendHeardBeat(){
        if (taskExecutionHeartBeatDTO != null){
            schedulerRestClientApi.lockHeartBeat(taskExecutionHeartBeatDTO);
            logger.info("Heart beat lockId={} sent", taskExecutionHeartBeatDTO.getLockId());
        }
    }

    public void start(TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO){
        this.taskExecutionHeartBeatDTO = taskExecutionHeartBeatDTO;
    }
    public void stop(){
        taskExecutionHeartBeatDTO = null;
    }
}
