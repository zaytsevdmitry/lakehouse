/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
