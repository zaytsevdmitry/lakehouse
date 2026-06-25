/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
public class HeardBeatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final Map<Long, TaskExecutionHeartBeatDTO> heartBeatMap = new HashMap<>();

    public HeardBeatService(
            SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
    }

    @Scheduled(
            fixedDelayString = "${lakehouse.task-executor.service.heart-beat-interval-ms}",
            initialDelayString = "${lakehouse.task-executor.service.heart-beat-initial-delay-ms}")
    public void sendHeardBeat() {
        heartBeatMap.values().forEach(taskExecutionHeartBeatDTO -> {
            logger.info("Prepare heard beat {}", taskExecutionHeartBeatDTO);
            schedulerRestClientApi.lockHeartBeat(taskExecutionHeartBeatDTO);
            logger.info("Heart beat lockId={} sent", taskExecutionHeartBeatDTO.getLockId());
        });
    }

    public void start(TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO) {
        heartBeatMap.put(taskExecutionHeartBeatDTO.getLockId(), taskExecutionHeartBeatDTO);
    }

    public void stop(TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO) {
        heartBeatMap.remove(taskExecutionHeartBeatDTO.getLockId());
    }
}
