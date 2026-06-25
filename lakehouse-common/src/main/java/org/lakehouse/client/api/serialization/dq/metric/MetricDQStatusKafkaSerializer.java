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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.support.SerializationFailedException;


public class MetricDQStatusKafkaSerializer implements Serializer<MetricDQStatusDTO> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public byte[] serialize(String topic, MetricDQStatusDTO data) {
        try {
            if (data == null) {
                logger.info("Null received at serializing");
                return null;
            }
            logger.info("Serializing...");
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new SerializationFailedException(String.format("Error when serializing %s to byte[]",MetricDQStatusDTO.class.getName()),e);
        }
    }


}
