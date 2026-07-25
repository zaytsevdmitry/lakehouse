package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StandaloneSparkAdapterTest {

    private final StandaloneSparkAdapter adapter = new StandaloneSparkAdapter("spark://master:7077", "http://localhost:6066");

    @Test
    void extractSubmissionId_found() throws CreateErrorException {
        String stdout = "Running Spark using the StandaloneApplicationMaster launcher.\n" +
                "Using main class: com.example.Main\n" +
                "driver-20250723123456-1234\n" +
                "Running Spark application...";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("driver-20250723123456-1234", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        String stdout = "Application started successfully without a driver ID";
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId(stdout));
    }

    @Test
    void buildSparkLauncher_setsMasterAndDeployMode() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of("arg1"), "s3://bucket/app.jar", null, "com.example.Main", Map.of("spark.executor.memory", "2g"), null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_noConf() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void buildSparkLauncher_noAppArgs() {
        Map<String, String> props = Map.of("spark.executor.memory", "2g");
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", props, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("spark://master:7077", adapter.masterUrl);
    }
}
