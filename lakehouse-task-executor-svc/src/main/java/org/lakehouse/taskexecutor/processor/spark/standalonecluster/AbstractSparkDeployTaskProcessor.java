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

package org.lakehouse.taskexecutor.processor.spark.standalonecluster;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.SparkRestClientApiImpl;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.lakehouse.taskexecutor.processor.AbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Based on spark restapi
 * @apiNote  <a href="https://spark.apache.org/docs/3.5.8/spark-standalone.html#rest-api">...</a>
 * restApi version 1 (/v1/submissions)
 * */
public abstract class AbstractSparkDeployTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String MAIN_CLASS_KEY = "deploy.mainClass";
    protected final String APP_RESOURCE_KEY = "deploy.appResource";
    protected final String CLUSTER_URL_KEY = "deploy.clusterUrl";
    private final String urnV1 = "/v1/submissions";

    public AbstractSparkDeployTaskProcessor() {
    }

    //todo other final status
    private final List<String> finalStatusNames = List.of("FINISHED", "KILLED", "FAILED", "ERROR");
    private final List<String> negativeStatusNames = List.of("KILLED", "FAILED" , "ERROR");

    private static final long RUNNING_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

    public boolean isStatusFinal(String statusName) {
        return finalStatusNames.contains(statusName);
    }

    public boolean isStatusNegative(String statusName) {
        return negativeStatusNames.contains(statusName);
    }



    public SparkRestClientApi buildSparkRestClientApi(String baseURI) {
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        RestClient restClient = RestClient.builder().uriBuilderFactory(defaultUriBuilderFactory).build();
        return new SparkRestClientApiImpl(new RestClientHelper(restClient));
    }


    public void deploy(
            String mainClass,
            String appResource,
            String severUrl,
            Map<String, String> sparkProperties,
            List<String> sparkArgs) throws TaskFailedException, TaskConfigurationException {
        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass(mainClass);
        createRequest.setAppResource(appResource);

        createRequest.setSparkProperties(sparkProperties);

        createRequest.setAppArgs(sparkArgs);

        CreateResponse createResponse = null;

        try {
            createResponse =buildSparkRestClientApi(severUrl).createSubmission(createRequest);
        } catch (RestClientResponseException e) {
            throw new TaskConfigurationException("Deploy failed. Response error", e);
        }

        logger.info(
                "Task  submitted as {}",
                createResponse.getSubmissionId());

        StatusResponse status = null;
        boolean isFinished = false;
        boolean isRunning = false;
        long startTime = System.currentTimeMillis();

        while (!isFinished) {
            sleep(1000L);
            status =
                    buildSparkRestClientApi(severUrl)
                            .getStatus(createResponse.getSubmissionId());
            logger.info("Spark job status {}", status.getDriverState());

            if (!isRunning && "RUNNING".equals(status.getDriverState())) {
                isRunning = true;
                logger.info("Spark job {} reached RUNNING state", createResponse.getSubmissionId());
            }

            if (isStatusFinal(status.getDriverState())) {
                isFinished = true;
            } else if (!isRunning
                    && System.currentTimeMillis() - startTime > RUNNING_TIMEOUT_MS) {
                throw new TaskFailedException(String.format(
                        "Spark job did not reach RUNNING state within %d seconds. Last status: %s",
                        TimeUnit.MILLISECONDS.toSeconds(RUNNING_TIMEOUT_MS),
                        status.getDriverState()));
            }
        }
        if (isStatusNegative(status.getDriverState()))
            throw new TaskFailedException(String.format("Spark job state is %s. %s", status.getDriverState(), status.getMessage()));
    }
    public String getMasterUrl(
            ScheduledTaskDTO scheduledTaskDTO) throws TaskConfigurationException {

        String result =  scheduledTaskDTO.getTaskProcessorArgs().getOrDefault(CLUSTER_URL_KEY,"");

        if ("".equals(result))
            throw new TaskConfigurationException(String.format("Cluster url is empty %s", CLUSTER_URL_KEY));

       return result;
    }
}
