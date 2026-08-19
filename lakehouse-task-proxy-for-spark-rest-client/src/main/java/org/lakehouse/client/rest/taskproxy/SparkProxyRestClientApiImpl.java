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

package org.lakehouse.client.rest.taskproxy;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.springframework.web.util.UriBuilder;

public class SparkProxyRestClientApiImpl implements SparkProxyRestClientApi {

    private final RestClientHelper restClientHelper;

    public SparkProxyRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public SubmissionResponse createSubmission(CreateSubmissionRequest request) {
        return restClientHelper.postDTO(request, Endpoint.SPARK_PROXY_SUBMISSIONS_CREATE, SubmissionResponse.class);
    }

    @Override
    public SubmissionStatusResponse getStatus(Long submissionId) {
        return restClientHelper.getDtoOne(String.valueOf(submissionId), Endpoint.SPARK_PROXY_SUBMISSIONS_STATUS, SubmissionStatusResponse.class);
    }

    @Override
    public SparkProxySubmissionsResponse getSubmissions(SparkProxySubmissionsRequest request) {
        return restClientHelper.getRestClient()
                .get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path(Endpoint.SPARK_PROXY_SUBMISSIONS_QUERY);
                    if (request.limit() != null) {
                        builder.queryParam("limit", request.limit());
                    }
                    if (request.lastId() != null) {
                        builder.queryParam("last_id", request.lastId());
                    }
                    if (request.id() != null) {
                        builder.queryParam("id", request.id());
                    }
                    if (request.status() != null) {
                        builder.queryParam("status", request.status());
                    }
                    if (request.dateFrom() != null) {
                        builder.queryParam("date_from", request.dateFrom());
                    }
                    if (request.dateTo() != null) {
                        builder.queryParam("date_to", request.dateTo());
                    }
                    return builder.build();
                })
                .retrieve()
                .body(SparkProxySubmissionsResponse.class);
    }

    @Override
    public SparkProxySubmissionPropertiesDTO getSparkProperties(Long id) {
        return restClientHelper.getDtoOne(String.valueOf(id), Endpoint.SPARK_PROXY_SUBMISSIONS_PROPERTIES, SparkProxySubmissionPropertiesDTO.class);
    }

    @Override
    public SubmissionResponse killSubmission(Long submissionId) {
        return restClientHelper.getRestClient()
                .post()
                .uri(Endpoint.SPARK_PROXY_SUBMISSIONS_KILL, submissionId)
                .retrieve()
                .body(SubmissionResponse.class);
    }

    @Override
    public SubmissionResponse killAllSubmissions() {
        return restClientHelper.getRestClient()
                .post()
                .uri(Endpoint.SPARK_PROXY_SUBMISSIONS_KILL_ALL)
                .retrieve()
                .body(SubmissionResponse.class);
    }

    @Override
    public SubmissionResponse clearCompleted() {
        return restClientHelper.getRestClient()
                .post()
                .uri(Endpoint.SPARK_PROXY_SUBMISSIONS_CLEAR)
                .retrieve()
                .body(SubmissionResponse.class);
    }
}