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

package org.lakehouse.config.produce;

import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.config.service.dq.QualityMetricsConfService;
import org.springframework.stereotype.Component;

@Component
public class QualityMetricsConfConfigurationResolver implements ConfigurationProduceResolver {

    public static final String KIND = "QualityMetricsConf";

    private final QualityMetricsConfService qualityMetricsConfService;

    public QualityMetricsConfConfigurationResolver(QualityMetricsConfService qualityMetricsConfService) {
        this.qualityMetricsConfService = qualityMetricsConfService;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public QualityMetricsConfDTO resolve(String keyName) {
        return qualityMetricsConfService.findById(keyName);
    }
}