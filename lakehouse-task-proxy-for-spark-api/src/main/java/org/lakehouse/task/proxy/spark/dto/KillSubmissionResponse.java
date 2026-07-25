package org.lakehouse.task.proxy.spark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KillSubmissionResponse(
    @JsonProperty("action") String action,
    @JsonProperty("message") String message,
    @JsonProperty("serverSparkVersion") String serverSparkVersion,
    @JsonProperty("submissionId") String submissionId,
    @JsonProperty("success") Boolean success
) {}
