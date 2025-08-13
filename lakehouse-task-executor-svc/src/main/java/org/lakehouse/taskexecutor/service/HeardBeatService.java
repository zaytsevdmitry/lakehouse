package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
public class HeardBeatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final Map<Long,TaskExecutionHeartBeatDTO> heartBeatMap = new HashMap<>();
    public HeardBeatService(
            SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
     }

    @Scheduled(
            fixedDelayString = "${lakehouse.task-executor.service.heart-beat-interval-ms}",
            initialDelayString = "${lakehouse.task-executor.service.heart-beat-initial-delay-ms}")
    public void sendHeardBeat(){
        heartBeatMap.values().forEach(taskExecutionHeartBeatDTO -> {
            logger.info("Prepare heard beat {}",taskExecutionHeartBeatDTO);
            schedulerRestClientApi.lockHeartBeat(taskExecutionHeartBeatDTO);
            logger.info("Heart beat lockId={} sent", taskExecutionHeartBeatDTO.getLockId());
        });
    }

    public void start(TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO){
        heartBeatMap.put(taskExecutionHeartBeatDTO.getLockId(),taskExecutionHeartBeatDTO);
    }
    public void stop(TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO) {
        heartBeatMap.remove(taskExecutionHeartBeatDTO.getLockId());
    }
}
