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
package org.lakehouse.task.proxy.spark.adapter;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandaloneSparkAdapter extends SparkAdapterBase {
    private  final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Pattern submissionIdPattern;

    private final String restUrl;

    public StandaloneSparkAdapter(String masterUrl, String restUrl, long submissionTimeoutSeconds, String submissionIdPattern) {
        super(masterUrl, submissionTimeoutSeconds);
        this.restUrl = restUrl;
        this.submissionIdPattern = Pattern.compile(submissionIdPattern);
        log.info("Initialised SparkAdapter {} with masterUrl: {}, control restUrl {}",
                StandaloneSparkAdapter.class.getSimpleName(),
                masterUrl,
                restUrl );
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        Matcher matcher = submissionIdPattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CreateErrorException("Standalone submission ID not found in output");
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        return defaultCreateSubmission(request);
    }

    @Override
    public SubmissionResponse killSubmission(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            return restClient.post()
                    .uri("/v1/submissions/kill/" + submissionId)
                    .retrieve()
                    .body(SubmissionResponse.class);
        } catch (Exception e) {
            log.error("Failed to kill standalone submission {}: {}", submissionId, e.getMessage(), e);
            return new SubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    @Override
    public SubmissionResponse killAllSubmissions() {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            return restClient.post()
                    .uri("/v1/submissions/killall")
                    .retrieve()
                    .body(SubmissionResponse.class);
        } catch (Exception e) {
            log.error("Failed to kill all standalone submissions: {}", e.getMessage(), e);
            return new SubmissionResponse("KillAllResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            SubmissionStatusResponse raw = restClient.get()
                    .uri("/v1/submissions/status/" + submissionId)
                    .retrieve()
                    .body(SubmissionStatusResponse.class);
            if (raw == null) {
                return new SubmissionStatusResponse("SparkStatusResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false, null, null, null);
            }

            return new SubmissionStatusResponse(
                    raw.action(),
                    raw.message(),
                    raw.serverSparkVersion(),
                    submissionId,
                    raw.success(),
                    raw.driverState(),
                    raw.workerId(),
                    raw.workerHostPort());
        } catch (Exception e) {
            log.error("Failed to get standalone submission status {}: {}", submissionId, e.getMessage(), e);
            return new SubmissionStatusResponse(
                    "SparkStatusResponse",
                    e.getMessage(),
                    null,
                    submissionId,
                    true,
                    ExternalStatus.UNKNOWN.name(),
                    null,
                    null);
        }
    }

    @Override
    public void postClear() {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            restClient.post()
                    .uri("/v1/submissions/clear")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Real standalone /clear succeeded");
        } catch (Exception e) {
            log.warn("Real standalone /clear not available (expected in Spark 3.5): {}", e.getMessage());
        }
    }
}
