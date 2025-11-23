package org.lakehouse.client.rest.spark;

import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;

public interface SparkRestClientApi {
    CreateResponse createSubmission(CreateRequest createRequest);

    StatusResponse getStatus(String submissionId);
}
