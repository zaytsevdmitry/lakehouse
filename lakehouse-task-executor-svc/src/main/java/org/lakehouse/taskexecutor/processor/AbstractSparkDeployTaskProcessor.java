package org.lakehouse.taskexecutor.processor;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.SparkRestClientApiImpl;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class AbstractSparkDeployTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public AbstractSparkDeployTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO
    ) {
        super(taskProcessorConfigDTO);
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
            String mainClass,
            String appResource,
            String severUrl,
            Map<String, String> sparkProperties,
            List<String> sparkArgs) throws TaskFailedException, TaskConfigurationException {
        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass(mainClass);
        createRequest.setAppResource(appResource);

        logger.info("Set task name to spark property spark.app.name");
        sparkProperties.put("spark.app.name", getTaskProcessorConfig().getTaskFullName());
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
                getTaskProcessorConfig().getLockSource(),
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
