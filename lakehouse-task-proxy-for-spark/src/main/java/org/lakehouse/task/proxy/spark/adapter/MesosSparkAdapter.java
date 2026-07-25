package org.lakehouse.task.proxy.spark.adapter;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

public class MesosSparkAdapter extends SparkAdapterBase {

    public MesosSparkAdapter(String masterUrl) {
        super(masterUrl);
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        throw new CreateErrorException("MESOS adapter not implemented");
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        throw new UnsupportedOperationException("MESOS adapter not implemented");
    }

    @Override
    public CreateSubmissionResponse killSubmission(String submissionId) {
        log.warn("MESOS killSubmission not yet implemented. submissionId={}", submissionId);
        return new CreateSubmissionResponse("KillResponse", "MESOS adapter not implemented", null, submissionId, false);
    }

    @Override
    public CreateSubmissionResponse killAllSubmissions() {
        log.warn("MESOS killAllSubmissions not yet implemented");
        return new CreateSubmissionResponse("KillAllResponse", "MESOS adapter not implemented", null, null, false);
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        log.warn("MESOS getSubmissionStatus not yet implemented. submissionId={}", submissionId);
        return new SubmissionStatusResponse("SparkStatusResponse", "MESOS adapter not implemented", null, submissionId, false, null, null, null);
    }

    @Override
    public CreateSubmissionResponse clearCompleted() {
        log.warn("MESOS clearCompleted not yet implemented");
        return new CreateSubmissionResponse("ClearResponse", "MESOS adapter not implemented", null, null, false);
    }
}
