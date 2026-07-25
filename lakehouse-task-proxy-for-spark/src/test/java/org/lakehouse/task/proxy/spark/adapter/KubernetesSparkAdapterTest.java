package org.lakehouse.task.proxy.spark.adapter;

import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesSparkAdapterTest {

    @Test
    void extractSubmissionId_podsPattern() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default");
        String stdout = "INFO: Submitted pods/spark-driver-abc123 from submission spark-app-xyz\n" +
                "Successfully created spark pod in namespace default";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("spark-driver-abc123", submissionId);
    }

    @Test
    void extractSubmissionId_driverPattern() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default");
        String stdout = "Starting driver pod driver-abc-123-def on k8s cluster\n" +
                "Pod created successfully";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("driver-abc-123-def", submissionId);
    }

    @Test
    void extractSubmissionId_podsPatternPreferredOverDriverPattern() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default");
        String stdout = "Submitted pods/spark-main-driver from spark driver-xyz\n" +
                "Pod created";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("spark-main-driver", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default");
        String stdout = "No submission info in this log line";
        assertThrows(CreateErrorException.class, () -> k8sAdapter.extractSubmissionId(stdout));
    }

    @Test
    void constructorSetsMasterUrl() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default");
        assertEquals("k8s://host:443", k8sAdapter.masterUrl);
    }
}
