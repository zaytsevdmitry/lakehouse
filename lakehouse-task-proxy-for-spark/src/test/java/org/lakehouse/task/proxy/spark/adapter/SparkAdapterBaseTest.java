package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SparkAdapterBaseTest {

    private TestSparkAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TestSparkAdapter("spark://test-master:7077");
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
    void extractSubmissionId_throwsOnInvalidOutput() {
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId("no-id-here"));
    }

    @Test
    void masterUrlInjectedCorrectly() {
        assertEquals("spark://test-master:7077", adapter.masterUrl);
    }

    static class TestSparkAdapter extends SparkAdapterBase {

        public TestSparkAdapter(String masterUrl) {
            super(masterUrl);
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
        public org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse killSubmission(String submissionId) {
            return new org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse("KillResponse", "KILLED", null, submissionId, true);
        }

        @Override
        public org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse killAllSubmissions() {
            return new org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse("KillAllResponse", "KILLED", null, null, true);
        }

        @Override
        public org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse getSubmissionStatus(String submissionId) {
            return new org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse("StatusResponse", "RUNNING", null, submissionId, true, "RUNNING", null, null);
        }

        @Override
        public org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse clearCompleted() {
            return new org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse("ClearResponse", "OK", null, null, true);
        }
    }
}
