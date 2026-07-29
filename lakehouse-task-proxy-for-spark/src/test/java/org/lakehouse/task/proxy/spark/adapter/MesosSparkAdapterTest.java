package org.lakehouse.task.proxy.spark.adapter;

import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import static org.junit.jupiter.api.Assertions.*;

class MesosSparkAdapterTest {

    private final MesosSparkAdapter adapter = new MesosSparkAdapter("mesos://master:5050", 30);

    @Test
    void extractSubmissionId_alwaysThrows() {
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId("any output"));
    }

    @Test
    void createSubmission_throwsUnsupported() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(null, null, null, null, null, null, null);
        assertThrows(UnsupportedOperationException.class, () -> adapter.createSubmission(request));
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("mesos://master:5050", adapter.masterUrl);
    }

    @Test
    void killSubmission_returnsNotImplemented() {
        var result = adapter.killSubmission("some-id");
        assertFalse(result.success());
        assertEquals("MESOS adapter not implemented", result.message());
    }

    @Test
    void getSubmissionStatus_returnsNotImplemented() {
        var result = adapter.getSubmissionStatus("some-id");
        assertEquals("MESOS adapter not implemented", result.message());
    }
}
