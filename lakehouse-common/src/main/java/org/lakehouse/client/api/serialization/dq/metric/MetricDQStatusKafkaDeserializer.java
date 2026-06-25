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
