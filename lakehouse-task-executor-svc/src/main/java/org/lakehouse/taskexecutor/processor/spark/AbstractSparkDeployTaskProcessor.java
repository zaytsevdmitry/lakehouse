package org.lakehouse.taskexecutor.processor.spark;

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

/**
 * Based on spark restapi
 * @apiNote  <a href="https://spark.apache.org/docs/3.5.8/spark-standalone.html#rest-api">...</a>
 * restApi version 1 (/v1/submissions)
 * */
public abstract class AbstractSparkDeployTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String MAIN_CLASS_KEY = "deploy.mainClass";
    protected final String APP_RESOURCE_KEY = "deploy.appResource";

    public AbstractSparkDeployTaskProcessor() {
    }

    //todo other final status
    private final List<String> finalStatusNames = List.of("FINISHED", "KILLED", "FAILED", "ERROR");
    private final List<String> negativeStatusNames = List.of("KILLED", "FAILED" , "ERROR");

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
            String taskFullName,
            String mainClass,
            String appResource,
            String severUrl,
            Map<String, String> sparkProperties,
            List<String> sparkArgs) throws TaskFailedException, TaskConfigurationException {
        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass(mainClass);
        createRequest.setAppResource(appResource);

        logger.info("Set task name to spark property spark.app.name");
        sparkProperties.put("spark.app.name", taskFullName);
        createRequest.setSparkProperties(sparkProperties);

        createRequest.setAppArgs(sparkArgs);

        CreateResponse createResponse = null;
        try {

            createResponse =buildSparkRestClientApi(severUrl).createSubmission(createRequest);

        } catch (RestClientResponseException e) {
            throw new TaskConfigurationException("Deploy failed. Response error", e);
        }
        logger.info(
                "Task {} submitted as {}",
                taskFullName,
                createResponse.getSubmissionId());

        StatusResponse status = null;
        boolean isFinished = false;
        while (!isFinished) {
            sleep(1000L);
            status =
                    buildSparkRestClientApi(severUrl)
                            .getStatus(createResponse.getSubmissionId());
            logger.info("Spark job status {}", status.getDriverState());
            if (isStatusFinal(status.getDriverState()))
                isFinished = true;
        }
        if (isStatusNegative(status.getDriverState()))
            throw new TaskFailedException(String.format("Spark job state is %s", status.getDriverState()));
    }



}
