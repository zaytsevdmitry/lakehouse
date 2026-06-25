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

package org.lakehouse.config.service;

import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.ScheduleProduceMessage;
import org.lakehouse.config.repository.ScheduleProduceMessageRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduleConfigProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate;
    private final String scheduleTopic;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleProduceMessageRepository scheduleProduceMessageRepository;

    public ScheduleConfigProducerService(
            KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate,
            @Value("${lakehouse.config.schedule.send.topic}") String scheduleTopic,
            ScheduleRepository scheduleRepository, ScheduleProduceMessageRepository scheduleProduceMessageRepository
    ) {
        this.scheduleEffectiveDTOKafkaTemplate = scheduleEffectiveDTOKafkaTemplate;
        this.scheduleTopic = scheduleTopic;
        this.scheduleRepository = scheduleRepository;
        this.scheduleProduceMessageRepository = scheduleProduceMessageRepository;

    }

    public void send(ScheduleEffectiveDTO msg) {
        scheduleEffectiveDTOKafkaTemplate.send(scheduleTopic, msg.getKeyName(), msg);
    }

    public void changeSchedule(Schedule schedule) {
        ScheduleProduceMessage scheduleProduceMessage = new ScheduleProduceMessage();
        scheduleProduceMessage.setSchedule(schedule);
        scheduleProduceMessageRepository.save(scheduleProduceMessage);
    }


}
