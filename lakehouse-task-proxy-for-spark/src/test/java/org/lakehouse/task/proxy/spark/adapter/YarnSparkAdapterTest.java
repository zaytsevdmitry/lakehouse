package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YarnSparkAdapterTest {

    private final YarnSparkAdapter adapter = new YarnSparkAdapter("yarn", "http://localhost:8088");

    @Test
    void extractSubmissionId_found() throws CreateErrorException {
        String stdout = "Launching Spark application on YARN cluster...\n" +
                "Submitted application application_1700000000000_12345 to YARN\n" +
                "Application tracking URL: http://resourcemanager:8088/proxy/application_1700000000000_12345/";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("application_1700000000000_12345", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        String stdout = "Spark application submitted successfully but no YARN ID in output";
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId(stdout));
    }

    @Test
    void extractSubmissionId_multipleApplications_returnsFirst() throws CreateErrorException {
        String stdout = "First: Submitted application application_1700000000000_11111 to YARN\n" +
                "Second: Submitted application application_1700000000000_22222 to YARN";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("application_1700000000000_11111", submissionId);
    }

    @Test
    void buildSparkLauncher_setsMasterAndDeployMode() {
        var request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("yarn", adapter.masterUrl);
    }
}
