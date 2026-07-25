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
