package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class AbstractSparkDeployTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SparkRestClientApi sparkRestClientApi;
    private final SparkConfigurationProperties sparkConfigurationProperties;

    public AbstractSparkDeployTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            SparkConfigurationProperties sparkConfigurationProperties,
            SparkRestClientApi sparkRestClientApi) {
        super(taskProcessorConfigDTO);
        this.sparkConfigurationProperties = sparkConfigurationProperties;
        this.sparkRestClientApi = sparkRestClientApi;

    }

    //todo other final status
    List<String> finalStatusNames = List.of("FINISHED", "KILLED", "FAILED");
    List<String> negativeStatusNames = List.of("KILLED", "FAILED");

    public boolean isStatusFinal(String statusName) {
        return finalStatusNames.contains(statusName);
    }

    public boolean isStatusNegative(String statusName) {
        return negativeStatusNames.contains(statusName);
    }

    public SparkRestClientApi getSparkRestClientApi() {
        return sparkRestClientApi;
    }

    public SparkConfigurationProperties getSparkConfigurationProperties() {
        return sparkConfigurationProperties;
    }

    public Map<String, String> extractConfSpark(Map<String, String> props) {
        logger.info("Use service spark properties");
        Map<String, String> result = new HashMap<>(getSparkConfigurationProperties().getProperties());
        logger.info("Set task name to spark property spark.app.name");
        result.put("spark.app.name", getTaskProcessorConfig().getLockSource());
        logger.info("Override service spark properties from task properties");
        result.putAll(
                getTaskProcessorConfig()
                        .getExecutionModuleArgs()
                        .entrySet()
                        .stream()
                        .filter(sse -> sse.getValue().startsWith("spark."))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        result.putAll(props);
        return result;
    }

    public Map<String, String> extractAppConf(Map<String, String> props) {
        Map<String, String> result = new HashMap<>();
        result.putAll(getTaskProcessorConfig().getExecutionModuleArgs()
                .entrySet()
                .stream()
                .filter(sse -> !sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        result.putAll(props.entrySet()
                .stream()
                .filter(sse -> !sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return result;
    }

    public void deploy(
            String mainClass,
            String appResource,
            Map<String, String> sparkProperties,
            List<String> sparkArgs) throws TaskFailedException {
        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass(mainClass);
        createRequest.setAppResource(appResource);
        createRequest.setSparkProperties(sparkProperties);

        logger.info("Remove spark properties from task properties");

        createRequest.setAppArgs(sparkArgs);

        CreateResponse createResponse =
                getSparkRestClientApi().createSubmission(createRequest);

        logger.info(
                "Task {} submitted as {}",
                getTaskProcessorConfig().getLockSource(),
                createResponse.getSubmissionId());

        StatusResponse status = null;
        boolean isFinished = false;
        while (!isFinished) {
            sleep(1000L);
            status =
                    getSparkRestClientApi()
                            .getStatus(createResponse.getSubmissionId());
            logger.info("Spark job status {}", status.getDriverState());
            if (isStatusFinal(status.getDriverState()))
                isFinished = true;
        }
        if (isStatusNegative(status.getDriverState()))
            throw new TaskFailedException(String.format("Spark job state is %s", status.getDriverState()));
    }
}
