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

package org.lakehouse.client.rest.taskproxy.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.rest.taskproxy.SparkProxyRestClientApi;
import org.lakehouse.client.rest.taskproxy.configuration.SparkProxyRestClientConfiguration;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsMeta;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(properties = {
        "lakehouse.client.rest.task-proxy-for-spark.server.url=",
})
@ContextConfiguration(classes = {SparkProxyRestClientConfiguration.class})
class SparkProxyRestClientTest {

    @Autowired
    SparkProxyRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    private SubmissionResponse submissionResponse() {
        return new SubmissionResponse(
                "SubmissionResponse",
                "Driver successfully submitted",
                "3.5.8",
                "driver-1",
                true);
    }

    private SubmissionStatusResponse submissionStatusResponse() {
        return new SubmissionStatusResponse(
                "SubmissionStatusResponse",
                "Driver successfully returned",
                "3.5.8",
                "driver-1",
                true,
                "FINISHED",
                "worker-1",
                "host:port");
    }

    @Test
    void createsSubmissionAgainstCreateEndpoint() throws Exception {
        Map<String, String> sparkProperties = new HashMap<>();
        sparkProperties.put("spark.master", "spark://master:7077");
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                "CreateSubmissionRequest",
                Arrays.asList("/app.py", "10"),
                "/opt/spark/examples",
                "3.5.8",
                "org.apache.spark.deploy.SparkSubmit",
                sparkProperties,
                new HashMap<>());

        SubmissionResponse responseExpect = submissionResponse();
        server.expect(requestTo(Endpoint.SPARK_PROXY_SUBMISSIONS_CREATE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SubmissionResponse response = client.createSubmission(request);

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }

    @Test
    void getsSubmissionStatusAgainstStatusEndpoint() throws Exception {
        SubmissionStatusResponse responseExpect = submissionStatusResponse();
        String endpoint = Endpoint.SPARK_PROXY_SUBMISSIONS_STATUS.replace("{submissionId}", "42");
        server.expect(requestTo(endpoint))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SubmissionStatusResponse response = client.getStatus(42L);

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }

    @Test
    void getsSubmissionsAgainstQueryEndpoint() throws Exception {
        SparkProxySubmissionDTO item = new SparkProxySubmissionDTO(
                1431L, "driver-20260814-0001", "RUNNING",
                "hdfs:///apps/spark-job.jar", "com.example.Main",
                List.of("--input", "/data"),
                "submitted", Instant.parse("2026-08-14T12:00:00Z"), Instant.parse("2026-08-14T12:05:00Z"));
        SparkProxySubmissionsResponse responseExpect = new SparkProxySubmissionsResponse(
                List.of(item),
                new SparkProxySubmissionsMeta(20, true, 1431L));

        SparkProxySubmissionsRequest request = new SparkProxySubmissionsRequest(
                20, 1431L, null, "RUNNING", "2026-08-14T00:00:00Z", "2026-08-14T23:59:59Z");

        String endpoint = "/api/v1/spark-proxy-submissions"
                + "?limit=20&last_id=1431&status=RUNNING"
                + "&date_from=2026-08-14T00:00:00Z&date_to=2026-08-14T23:59:59Z";
        server.expect(requestTo(endpoint))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SparkProxySubmissionsResponse response = client.getSubmissions(request);

        assertThat(response).isEqualTo(responseExpect);
        assertThat(response.items().get(0).submissionId()).isEqualTo("driver-20260814-0001");
        assertThat(response.items().get(0).createdAt()).isEqualTo(Instant.parse("2026-08-14T12:00:00Z"));
        server.verify();
    }

    @Test
    void getsSparkPropertiesAgainstPropertiesEndpoint() throws Exception {
        SparkProxySubmissionPropertiesDTO responseExpect = new SparkProxySubmissionPropertiesDTO(
                1431L, "driver-20260814-0001", Map.of("spark.cores", "2"));
        String endpoint = Endpoint.SPARK_PROXY_SUBMISSIONS_PROPERTIES.replace("{id}", "1431");
        server.expect(requestTo(endpoint))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SparkProxySubmissionPropertiesDTO response = client.getSparkProperties(1431L);

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }

    @Test
    void killsSubmissionAgainstKillEndpoint() throws Exception {
        SubmissionResponse responseExpect = submissionResponse();
        String endpoint = Endpoint.SPARK_PROXY_SUBMISSIONS_KILL.replace("{submissionId}", "42");
        server.expect(requestTo(endpoint))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SubmissionResponse response = client.killSubmission(42L);

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }

    @Test
    void killsAllSubmissionsAgainstKillAllEndpoint() throws Exception {
        SubmissionResponse responseExpect = submissionResponse();
        server.expect(requestTo(Endpoint.SPARK_PROXY_SUBMISSIONS_KILL_ALL))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SubmissionResponse response = client.killAllSubmissions();

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }

    @Test
    void clearsCompletedSubmissionsAgainstClearEndpoint() throws Exception {
        SubmissionResponse responseExpect = submissionResponse();
        server.expect(requestTo(Endpoint.SPARK_PROXY_SUBMISSIONS_CLEAR))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseExpect), MediaType.APPLICATION_JSON));

        SubmissionResponse response = client.clearCompleted();

        assertThat(response).isEqualTo(responseExpect);
        server.verify();
    }
}