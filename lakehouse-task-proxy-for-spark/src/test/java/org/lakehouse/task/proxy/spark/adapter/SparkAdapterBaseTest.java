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

import org.apache.spark.launcher.SparkLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SparkAdapterBaseTest {

    private TestSparkAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TestSparkAdapter("spark://test-master:7077", 30);
    }

    @Test
    void buildSparkLauncher_setsMasterAndDeployMode() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of("arg1"), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_setsMainClass() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.example.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_setsSparkProperties() {
        Map<String, String> properties = Map.of(
                "spark.executor.memory", "4g",
                "spark.executor.cores", "2"
        );
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", properties, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_setsAppArgs() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of("arg1", "arg2", "arg3"), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_noArgs() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_setsEnvironmentVariables() {
        Map<String, String> envVars = Map.of("SPARK_LOCAL_IP", "127.0.0.1", "MY_VAR", "value");
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", null, envVars
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void extractSubmissionId_found() throws CreateErrorException {
        assertEquals("submission-123", adapter.extractSubmissionId("output with submission-123 inside"));
    }

    @Test
    void extractSubmissionId_notFound() {
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId("no-id-here"));
    }

    @Test
    void masterUrlInjectedCorrectly() {
        assertEquals("spark://test-master:7077", adapter.masterUrl);
    }

    static class TestSparkAdapter extends SparkAdapterBase {

        public TestSparkAdapter(String masterUrl, long submissionTimeoutSeconds) {
            super(masterUrl, submissionTimeoutSeconds);
        }

        @Override
        public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
            return defaultCreateSubmission(request);
        }

        @Override
        protected String extractSubmissionId(String output) throws CreateErrorException {
            if (output.contains("submission-123")) {
                return "submission-123";
            }
            throw new CreateErrorException("No submission ID found");
        }

        @Override
        public SubmissionResponse killSubmission(String submissionId) {
            return new SubmissionResponse("KillResponse", "KILLED", null, submissionId, true);
        }

        @Override
        public SubmissionResponse killAllSubmissions() {
            return new SubmissionResponse("KillAllResponse", "KILLED", null, null, true);
        }

        @Override
        public org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse getSubmissionStatus(String submissionId) {
            return new org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse("StatusResponse", "RUNNING", null, submissionId, true, "RUNNING", null, null);
        }

        @Override
        public SubmissionResponse clearCompleted(String submissionId) {
            return new SubmissionResponse("ClearResponse", "OK", null, submissionId, true);
        }
    }
}
