package org.lakehouse.taskexecutor.executionmodule;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkLauncherTaskProcessor extends AbstractSparkDeployTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public String JDK_JAVA_OPTIONS=
            " --add-exports=java.base/sun.nio.ch=ALL-UNNAMED" +
                    " --add-opens=java.base/java.io=ALL-UNNAMED" +
                    " --add-opens=java.base/java.lang.invoke=ALL-UNNAMED" +
                    " --add-opens=java.base/java.lang.reflect=ALL-UNNAMED" +
                    " --add-opens=java.base/java.lang=ALL-UNNAMED" +
                    " --add-opens=java.base/java.net=ALL-UNNAMED" +
                    " --add-opens=java.base/java.nio=ALL-UNNAMED" +
                    " --add-opens=java.base/java.sql=ALL-UNNAMED" +
                    " --add-opens=java.sql/java.sql=ALL-UNNAMED" +
                    " --add-opens=java.base/java.util.concurrent.atomic=ALLSparkConfigurationProperties-UNNAMED" +
                    " --add-opens=java.base/java.util.concurrent=ALL-UNNAMED" +
                    " --add-opens=java.base/java.util=ALL-UNNAMED" +
                    " --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED" +
                    " --add-opens=java.base/sun.nio.ch=ALL-UNNAMED" +
                    " --add-opens=java.base/sun.security.action=ALL-UNNAMED" +
                    " --add-opens=java.base/sun.util.calendar=ALL-UNNAMED" +
                    " --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED" +
                    " --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED" +
                    " -Djdk.reflect.useDirectMethodHandle=false" +
                    " -XX:+IgnoreUnrecognizedVMOptions";
    public SparkLauncherTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            SparkConfigurationProperties sparkConfigurationProperties,
            SparkRestClientApi sparkRestClientApi) {
        super(taskProcessorConfigDTO,sparkConfigurationProperties, sparkRestClientApi);
    }

    @Override
    public void runTask() throws TaskFailedException {

        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass("org.lakehouse.taskexecutor.executionmodule.body.SparkProcessorBodyStarter");
        createRequest.setAppResource("/home/dm/projects/my/lakehouse/lakehouse-task-spark-apps/target/lakehouse-task-spark-apps-0.3.0.jar");
        logger.info("Use service spark properties");
        Map<String, String> sparkConf = new HashMap<>(getSparkConfigurationProperties().getProperties());
        logger.info("Set task name to spark property spark.app.name");
        sparkConf.put("spark.app.name", getTaskProcessorConfig().getLockSource());
        logger.info("Override service spark properties from task properties");
        sparkConf.putAll(
                getTaskProcessorConfig()
                        .getExecutionModuleArgs()
                        .entrySet()
                        .stream()
                        .filter(sse -> sse.getValue().startsWith("spark."))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        createRequest.setSparkProperties(sparkConf);

        logger.info("Remove spark properties from task properties");
        TaskProcessorConfigDTO unSparkedConfig = getTaskProcessorConfig();
        unSparkedConfig.setExecutionModuleArgs(
                unSparkedConfig.getExecutionModuleArgs()
                        .entrySet()
                        .stream()
                        .filter(sse -> !sse.getValue().startsWith("spark."))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));


        try{
            createRequest.setAppArgs(List.of(ObjectMapping.asJsonString(unSparkedConfig)));
        } catch (JsonProcessingException e) {
            throw new TaskFailedException(e);
        }

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
    //todo other final status
    List<String> finalStatusNames = List.of("FINISHED","KILLED", "FAILED");
    List<String> negativeStatusNames = List.of("KILLED", "FAILED");

    private boolean isStatusFinal(String statusName){
        return finalStatusNames.contains(statusName);
    }
    private boolean isStatusNegative(String statusName){
        return negativeStatusNames.contains(statusName);
    }
}
