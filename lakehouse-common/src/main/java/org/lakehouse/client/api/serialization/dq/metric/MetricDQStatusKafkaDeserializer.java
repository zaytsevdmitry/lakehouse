package org.lakehouse.client.api.serialization.dq.metric;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricDQStatusKafkaDeserializer implements Deserializer<MetricDQStatusDTO> {


    final private  Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public MetricDQStatusDTO deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                logger.info("Null received at deserializing");
                return null;
            }
            logger.info("Deserializing...");
            return ObjectMapping.stringToObject(data, MetricDQStatusDTO.class);
        } catch (Exception e) {
            throw new SerializationException("Error when deserializing byte[] to MessageDto");
        }
    }

}
