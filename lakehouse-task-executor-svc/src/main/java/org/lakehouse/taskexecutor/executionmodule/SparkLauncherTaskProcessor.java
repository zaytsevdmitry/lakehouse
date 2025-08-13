package org.lakehouse.taskexecutor.executionmodule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class SparkLauncherTaskProcessor extends AbstractDefaultTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SparkLauncherTaskProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
    }

    @Override
    public void runTask() throws TaskFailedException {

        org.apache.spark.launcher.SparkAppHandle handle = getSparkAppHandle();
        while (handle.getAppId() == null
                && !handle.getState().equals(SparkAppHandle.State.FAILED)
           ) {
            logger.info("Waiting for application to be submitted: status={}", handle.getState());
            sleep1500ms();
        }
        if(handle.getState().equals(SparkAppHandle.State.FAILED))
            throw new TaskFailedException("Submit failed");
// todo Exception in thread "main" java.lang.NoClassDefFoundError: com/ctc/wstx/io/InputBootstrapper
       // look maven tree deps
        
        logger.info("Submitted as {}", handle.getAppId());

        while (!handle.getState().isFinal()) {
                logger.info("{}: status={}", handle.getAppId(), handle.getState());
                sleep1500ms();
            }
        logger.info("Finished as {}", handle.getState());

    }
    private void sleep1500ms()    throws TaskFailedException{
        try{
            Thread.sleep(1500L);
        }catch (InterruptedException e){
            throw new TaskFailedException("Sleep failed",e);
        }
    }
    private String getAppArg() throws TaskFailedException {
        try {
            return ObjectMapping.asJsonString(getTaskProcessorConfig());
        } catch (JsonProcessingException e) {
            throw new TaskFailedException(e);
        }

    }
    private SparkAppHandle getSparkAppHandle() throws TaskFailedException {
        SparkLauncher launcher = new SparkLauncher()
                .setVerbose(true)
        //.addJar("hdfs:///user/user/jars/log4j-api-2.7.jar")
        //.addJar("hdfs:///user/user/jars/log4j-core-2.7.jar")
                .setAppResource("/opt/bitnami/spark/jars/lakehouse-task-spark-apps-0.3.0-jar-with-dependencies.jar")
                .setMainClass("org.lakehouse.taskexecutor.executionmodule.body.SparkProcessorBodyStarter")
                .setConf("spark.master", "spark://127.0.0.1:7077")
                .addAppArgs(getAppArg())
                .setDeployMode("cluster")
                .addAppArgs(getAppArg())
                .redirectError()
               .redirectOutput(new File("spark-launcher.out"));
        //todo missed in configuration
        // .redirectError(new File("submit.err.log"))
        // .redirectOutput(new File("submit.out.log"));

        getTaskProcessorConfig()
                .getExecutionModuleArgs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("spark."))
                .forEach(stringStringEntry ->
                        launcher.setConf(stringStringEntry.getKey(),stringStringEntry.getValue()));

        try {
            return launcher.startApplication();
        } catch (IOException e) {
            throw new TaskFailedException("Application start error", e);
        }
    }
}
