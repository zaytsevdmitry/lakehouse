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

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskDTOProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<Long, ScheduledTaskMsgDTO> kafkaTemplate;
    private final String scheduledTaskTopic;

    public ScheduledTaskDTOProducerService(
            KafkaTemplate<Long, ScheduledTaskMsgDTO> kafkaTemplate,
            @Value("${lakehouse.scheduler.schedule.task.kafka.producer.topic}") String scheduledTaskTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.scheduledTaskTopic = scheduledTaskTopic;
    }

    public void send(ScheduledTaskMsgDTO msg) {
        kafkaTemplate.send(scheduledTaskTopic, msg.getId(), msg);
    }


}
