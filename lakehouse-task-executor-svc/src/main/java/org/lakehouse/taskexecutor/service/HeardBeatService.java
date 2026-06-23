/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
