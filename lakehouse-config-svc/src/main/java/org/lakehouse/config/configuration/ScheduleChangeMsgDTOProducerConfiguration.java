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

package org.lakehouse.config.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduleChangeMsgDTOProducerConfiguration {

    @Bean
    public KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate(
            ScheduleConfKafkaProducerConfigurationProperties scheduleConfKafkaProducerConfigurationProperties) {
        Map<String, Object> props = new HashMap<>(scheduleConfKafkaProducerConfigurationProperties.getProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaSerializer.class);
        ProducerFactory<String, ScheduleEffectiveDTO> producerFactory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<String, ScheduleEffectiveDTO>(producerFactory);
    }


}
