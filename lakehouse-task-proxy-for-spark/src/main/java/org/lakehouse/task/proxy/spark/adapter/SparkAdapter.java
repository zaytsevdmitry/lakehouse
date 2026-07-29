package org.lakehouse.task.proxy.spark.adapter;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

public interface SparkAdapter {

    String createSubmission(CreateSubmissionRequest request) throws CreateErrorException;

    SubmissionResponse killSubmission(String submissionId);

    SubmissionResponse killAllSubmissions();

    SubmissionStatusResponse getSubmissionStatus(String submissionId);

    SubmissionResponse clearCompleted(String submissionId);

    default void postClear() {
    }
}
