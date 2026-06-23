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
