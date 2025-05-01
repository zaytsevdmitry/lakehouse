package org.lakehouse.client.api.serialization.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.support.SerializationFailedException;


public class ScheduleEffectiveKafkaSerializer implements Serializer<ScheduleEffectiveDTO> {
        private final ObjectMapper objectMapper = new ObjectMapper();
        final private Logger logger = LoggerFactory.getLogger(this.getClass());

        @Override
        public byte[] serialize(String topic, ScheduleEffectiveDTO data) {
            try {
                if (data == null){
                    System.out.println("Null received at serializing");
                    return null;
                }
                System.out.println("Serializing...");
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new SerializationFailedException("Error when serializing MessageDto to byte[]");
            }
        }


}
