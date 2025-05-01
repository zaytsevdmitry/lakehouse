package org.lakehouse.client.api.serialization.schedule;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.ObjectMapping;

import java.util.Map;

public class ScheduleEffectiveKafkaDeserializer implements Deserializer<ScheduleEffectiveDTO> {


    @Override
    public ScheduleEffectiveDTO deserialize(String topic, byte[] data) {
        try {
            if (data == null){
                System.out.println("Null received at deserializing");
                return null;
            }
            System.out.println("Deserializing...");
            return ObjectMapping.stringToObject(data, ScheduleEffectiveDTO.class);
        } catch (Exception e) {
            throw new SerializationException("Error when deserializing byte[] to MessageDto");
        }
    }

}
