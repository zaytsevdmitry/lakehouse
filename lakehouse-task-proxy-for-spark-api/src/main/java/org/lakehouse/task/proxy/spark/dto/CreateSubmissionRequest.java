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
package org.lakehouse.task.proxy.spark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateSubmissionRequest(
    @JsonProperty("action") String action,
    @JsonProperty("appArgs") List<String> appArgs,
    @JsonProperty("appResource") String appResource,
    @JsonProperty("clientSparkVersion") String clientSparkVersion,
    @JsonProperty("mainClass") String mainClass,
    @JsonProperty("sparkProperties") Map<String, String> sparkProperties,
    @JsonProperty("environmentVariables") Map<String, String> environmentVariables
) {
    // Компактный конструктор для автоматической установки поля action
    public CreateSubmissionRequest {
        if (action == null) {
            action = "CreateSubmissionRequest";
        }
    }

    // Удобный перегруженный конструктор для создания запроса без ручного указания action
    public CreateSubmissionRequest(List<String> appArgs, String appResource, String clientSparkVersion, 
                                   String mainClass, Map<String, String> sparkProperties, 
                                   Map<String, String> environmentVariables) {
        this("CreateSubmissionRequest", appArgs, appResource, clientSparkVersion, mainClass, sparkProperties, environmentVariables);
    }
}
