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

package org.lakehouse.config.service;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.config.entities.ConfigurationProduceMessage;
import org.lakehouse.config.repository.ConfigurationProduceMessageRepository;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationProduceService {

    private final ConfigurationProduceMessageRepository configurationProduceMessageRepository;

    public ConfigurationProduceService(ConfigurationProduceMessageRepository configurationProduceMessageRepository) {
        this.configurationProduceMessageRepository = configurationProduceMessageRepository;
    }

    public void produce(String kind, String keyName, Types.configAction action) {
        ConfigurationProduceMessage message =
                new ConfigurationProduceMessage(kind, keyName, DateTimeUtils.now(), action);
        configurationProduceMessageRepository.save(message);
    }
}