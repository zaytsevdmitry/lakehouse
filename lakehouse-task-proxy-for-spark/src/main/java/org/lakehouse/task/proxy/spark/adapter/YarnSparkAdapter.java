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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YarnSparkAdapter extends SparkAdapterBase {
    private  final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String API_BASE = "/ws/v1/cluster";
    private final Pattern yarnAppPattern;

    private final String restUrl;

    public YarnSparkAdapter(String masterUrl, String restUrl, long submissionTimeoutSeconds, String submissionIdPattern) {
        super(masterUrl, submissionTimeoutSeconds);
        this.restUrl = restUrl;
        this.yarnAppPattern = Pattern.compile(submissionIdPattern);
        log.info("Initialised SparkAdapter is {} with masterUrl:{} , control restUrl {}",
                StandaloneSparkAdapter.class.getSimpleName(),
                masterUrl,
                restUrl );
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        Matcher matcher = yarnAppPattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CreateErrorException("YARN application ID not found in output");
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        return defaultCreateSubmission(request);
    }

    @Override
    public SubmissionResponse killSubmission(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            YarnAppInfo app = getApp(restClient, submissionId);
            if (app == null) {
                return new SubmissionResponse("KillResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false);
            }
            restClient.method(HttpMethod.DELETE)
                    .uri(API_BASE + "/apps/{appId}", submissionId)
                    .retrieve()
                    .body(YarnKillResponse.class);
            log.info("Killed YARN application {}", submissionId);
            return new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, submissionId, true);
        } catch (Exception e) {
            log.error("Failed to kill YARN application {}: {}", submissionId, e.getMessage(), e);
            return new SubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    @Override
    public SubmissionResponse killAllSubmissions() {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            List<YarnAppInfo> apps = listApps(restClient, "NEW,NEW_SAVING,SUBMITTED,ACCEPTED,RUNNING");
            int killed = 0;
            for (YarnAppInfo app : apps) {
                try {
                    restClient.method(HttpMethod.DELETE)
                            .uri(API_BASE + "/apps/{appId}", app.id)
                            .retrieve()
                            .body(YarnKillResponse.class);
                    killed++;
                    log.info("Killed YARN application {}", app.id);
                } catch (Exception e) {
                    log.error("Failed to kill YARN application {}: {}", app.id, e.getMessage(), e);
                }
            }
            log.warn("Killed {} YARN applications", killed);
            return new SubmissionResponse("KillAllResponse", ExternalStatus.KILLED.name() + " " + killed + " apps", null, null, true);
        } catch (Exception e) {
            log.error("Failed to list/kill YARN apps: {}", e.getMessage(), e);
            return new SubmissionResponse("KillAllResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            YarnAppInfo app = getApp(restClient, submissionId);
            if (app == null) {
                return new SubmissionStatusResponse("SparkStatusResponse", ExternalStatus.WAITING.name(), null, submissionId, true, null, null, null);
            }
            ExternalStatus external = ExternalStatus.fromYarnState(app.state);
            boolean success = external == ExternalStatus.RUNNING || external == ExternalStatus.FINISHED;
            log.debug("Status for YARN app {}: state={} -> {}", submissionId, app.state, external);
            return new SubmissionStatusResponse("SparkStatusResponse", external.name(), null, submissionId, success, app.state, null, null);
        } catch (Exception e) {
            log.error("Failed to get YARN app status {}: {}", submissionId, e.getMessage(), e);
            return new SubmissionStatusResponse("SparkStatusResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false, null, null, null);
        }
    }

    private YarnAppInfo getApp(RestClient restClient, String appId) {
        try {
            YarnAppResponse response = restClient.get()
                    .uri(API_BASE + "/apps/{appId}", appId)
                    .retrieve()
                    .body(YarnAppResponse.class);
            return response != null ? response.app : null;
        } catch (Exception e) {
            log.error("Failed to get YARN app {}: {}", appId, e.getMessage(), e);
            return null;
        }
    }

    private List<YarnAppInfo> listApps(RestClient restClient, String states) {
        try {
            YarnAppsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(API_BASE + "/apps")
                            .queryParam("states", states)
                            .build())
                    .retrieve()
                    .body(YarnAppsResponse.class);
            if (response == null || response.apps == null || response.apps.app == null) {
                return Collections.emptyList();
            }
            return response.apps.app;
        } catch (Exception e) {
            log.error("Failed to list YARN apps (states={}): {}", states, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class YarnAppResponse {
        @JsonProperty("app")
        YarnAppInfo app;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class YarnAppsResponse {
        @JsonProperty("apps")
        YarnAppsList apps;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class YarnAppsList {
        @JsonProperty("app")
        List<YarnAppInfo> app;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class YarnAppInfo {
        @JsonProperty("id")
        String id;
        @JsonProperty("state")
        String state;
        @JsonProperty("finalStatus")
        String finalStatus;
        @JsonProperty("applicationType")
        String applicationType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class YarnKillResponse {
        @JsonProperty("state")
        String state;
    }
}
