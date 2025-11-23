package org.lakehouse.client.rest.spark;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.springframework.stereotype.Service;

/**
 * Todo add implementations for all master variations
 * local:
 * Runs Spark locally with one thread. This is useful for testing and development on a single machine without a distributed cluster.
 * local[K]:
 * Runs Spark locally with K threads. For example, local[4] would run with 4 threads, utilizing multiple cores on your local machine.
 * local[*]:
 * Runs Spark locally with as many threads as there are logical cores on your machine. This maximizes local parallelization.
 * spark://HOST:PORT:
 * Connects to a Spark Standalone cluster manager running on the specified HOST and PORT. The default port for the Spark master is 7077.
 * yarn:
 * Connects to a YARN (Yet Another Resource Negotiator) cluster manager. Spark applications will be run on the YARN cluster.
 * mesos://HOST:PORT:
 * Connects to a Mesos cluster manager running on the specified HOST and PORT.
 * k8s://https://KUBERNETES_API_SERVER_HOST:PORT:
 * Connects to a Kubernetes cluster. This specifies the API server endpoint of your Kubernetes cluster.
 * Example Usage:
 * You can set spark.master in various ways: During spark-submit.
 */
@Service
public class SparkRestClientApiImpl implements SparkRestClientApi {
    private final RestClientHelper restClientHelper;

    public SparkRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public CreateResponse createSubmission(CreateRequest createRequest) {
        return restClientHelper
                .postDTO(
                        createRequest,
                        "/create",
                        CreateResponse.class);
    }

    @Override
    public StatusResponse getStatus(String submissionId) {
        return restClientHelper
                .getDtoOne(String.format("/status/%s", submissionId), StatusResponse.class);
    }
}
