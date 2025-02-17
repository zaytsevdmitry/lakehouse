package org.lakehouse.client.api.serialization.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.springframework.core.serializer.support.SerializationFailedException;

import java.util.Map;
/*
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
*/

public class ScheduledTaskMsgKafkaSerializer implements Serializer<ScheduledTaskMsgDTO> {
        private final ObjectMapper objectMapper = new ObjectMapper();
      //x  private final Logger logger = LoggerFactory.getLogger(this.getClass());
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, ScheduledTaskMsgDTO data) {
            try {
                if (data == null){
                    System.out.println("Null received at serializing");
                    return null;
                }
                System.out.println("Serializing...");
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SerializationFailedException("Error when serializing MessageDto to byte[]");
            }
        }

        @Override
        public void close() {
        }

}
