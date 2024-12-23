package org.lakehouse.cli.api.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.lakehouse.cli.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ScheduleEffectiveKafkaDeserializer implements Deserializer<ScheduleEffectiveDTO> {


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

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

    @Override
    public void close() {
    }
}
