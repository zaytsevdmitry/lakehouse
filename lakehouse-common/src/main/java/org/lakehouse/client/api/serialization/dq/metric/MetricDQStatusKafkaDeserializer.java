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
