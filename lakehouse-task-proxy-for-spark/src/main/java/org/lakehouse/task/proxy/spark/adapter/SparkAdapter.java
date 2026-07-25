package org.lakehouse.task.proxy.spark.adapter;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

public interface SparkAdapter {

    String createSubmission(CreateSubmissionRequest request) throws CreateErrorException;

    CreateSubmissionResponse killSubmission(String submissionId);

    CreateSubmissionResponse killAllSubmissions();

    SubmissionStatusResponse getSubmissionStatus(String submissionId);

    CreateSubmissionResponse clearCompleted();
}
