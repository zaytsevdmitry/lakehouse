package org.lakehouse.task.proxy.spark.adapter;

import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesSparkAdapterTest {

    @Test
    void extractSubmissionId_driverPodNamePattern() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "INFO KubernetesClusterSubmissionClient: driver pod name: spark-analytics-job-123456-driver\n" +
                "Successfully submitted";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("spark-analytics-job-123456-driver", submissionId);
    }

    @Test
    void extractSubmissionId_driverPodNamePatternWithHyphens() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "Submitted application driver pod name: 18-0-regular-transaction-dds-prepare-20250102T000000Z-driver\n" +
                "Pod created successfully";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("18-0-regular-transaction-dds-prepare-20250102T000000Z-driver", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "No submission info in this log line";
        assertThrows(CreateErrorException.class, () -> k8sAdapter.extractSubmissionId(stdout));
    }

    @Test
    void extractSubmissionId_emptyOutput() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        assertThrows(CreateErrorException.class, () -> k8sAdapter.extractSubmissionId(""));
    }

    @Test
    void constructorSetsMasterUrl() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        assertEquals("k8s://host:443", k8sAdapter.masterUrl);
    }
}
