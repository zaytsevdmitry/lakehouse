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

package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.ConfigurationChangeDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class ConfigurationChangeConsumerService {

    public static final String SCHEDULE_KIND = "Schedule";
    public static final String TASK_GROUP_KIND = "TaskExecutionServiceGroup";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BuildService buildService;
    private final ScheduleEffectiveService scheduleEffectiveService;
    private final TaskExecutionServiceGroupConfigService taskExecutionServiceGroupConfigService;

    public ConfigurationChangeConsumerService(
            BuildService buildService,
            ScheduleEffectiveService scheduleEffectiveService,
            TaskExecutionServiceGroupConfigService taskExecutionServiceGroupConfigService) {
        this.buildService = buildService;
        this.scheduleEffectiveService = scheduleEffectiveService;
        this.taskExecutionServiceGroupConfigService = taskExecutionServiceGroupConfigService;
    }

    @KafkaListener(
            topics = "#{'${lakehouse.scheduler.config.change.kafka.consumer.topics}'.split(',')}",
            concurrency = "#{'${lakehouse.scheduler.config.change.kafka.consumer.concurrency}'}",
            containerFactory = "configurationChangeContainerFactory")
    public void listen(ConfigurationChangeDTO changeDTO, Acknowledgment acknowledgment) throws Exception {
        logger.info("New config change: {}/{} ({})", changeDTO.getKind(), changeDTO.getKeyName(), changeDTO.getAction());

        switch (changeDTO.getKind()) {
            case SCHEDULE_KIND -> handleScheduleChange(changeDTO);
            case TASK_GROUP_KIND -> handleTaskGroupChange(changeDTO);
            default -> logger.info("Config change for kind {} is out of scheduler scope, ignored", changeDTO.getKind());
        }

        acknowledgment.acknowledge();
    }

    private void handleScheduleChange(ConfigurationChangeDTO changeDTO) throws IOException {
        if (changeDTO.getAction() == Types.configAction.DELETE) {
            logger.info("Schedule {} deleted, registration unhandled", changeDTO.getKeyName());
            return;
        }

        if (changeDTO.getAction() == Types.configAction.SAVE) {
            Map<?, ?> objectMap = castToMap(changeDTO);
            if (objectMap == null) return;

            ScheduleEffectiveDTO dto = ObjectMapping.mapToObject(objectMap, ScheduleEffectiveDTO.class);
            buildService.registration(dto);
            scheduleEffectiveService.setScheduleEffectiveDTO(dto);
            logger.info("Schedule {} registered", dto.getKeyName());
        }
    }

    private void handleTaskGroupChange(ConfigurationChangeDTO changeDTO) throws IOException {
        Map<?, ?> objectMap = castToMap(changeDTO);
        if (objectMap == null) return;


        TaskExecutionServiceGroupDTO dto = ObjectMapping.mapToObject(objectMap, TaskExecutionServiceGroupDTO.class);

        if (changeDTO.getAction() == Types.configAction.SAVE) {
            taskExecutionServiceGroupConfigService.addTaskExecutionServiceGroupDTO(dto);
        } else if (changeDTO.getAction() == Types.configAction.DELETE) {
            taskExecutionServiceGroupConfigService.deleteTaskExecutionServiceGroupDTO(dto.getName());
        }
    }

    private Map<?, ?> castToMap(ConfigurationChangeDTO changeDTO) {
        if (changeDTO.getObject() instanceof Map<?, ?> objectMap) {
            return objectMap;
        }
        logger.warn("Config change {}/{} has no object, registration skipped", changeDTO.getKind(), changeDTO.getKeyName());
        return null;
    }
}
