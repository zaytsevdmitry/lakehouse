package org.lakehouse.task.executor.spark.api.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.task.ScheduledTaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ApplicationBodyStarter {
    private final  Logger logger = LoggerFactory.getLogger(ApplicationBodyStarter.class);


    public void runAndStop(String[] args, Class<?> aClass) throws InterruptedException {
        ConfigurableApplicationContext context = null;
        int exitcode = ExitCode.success.getValue();
        try {
            logger.info("Create context");
            Arrays.asList(args).forEach(logger::info);
            context = run(args,aClass);

        } catch (TaskConfigurationException e) {
            logger.error("Task configuration failed",e);
            exitcode = ExitCode.TaskConfigurationException.getValue();

        } catch (TaskFailedException e) {
            logger.error("Task execution failed", e);
            exitcode = ExitCode.TaskFailedException.getValue();
        } catch(Exception e) {
            logger.error("Task failed by unexpected cause", e);
            exitcode = ExitCode.other.getValue();
        } finally {
            if (context!=null) {
                SparkSession sparkSession = context.getBean(SparkSession.class);
                logger.info("Stopping Spark session");
                sparkSession.stop();
                int maxAttempts = 10;
                while (!sparkSession.sparkContext().isStopped() && maxAttempts > 0) {
                    logger.info("Awaiting spark session.");
                    Thread.sleep(3000L); //todo made app parameter
                    maxAttempts--;
                }
                logger.info("Stopping Spring context");
                context.stop();
            }
            logger.info("Exit application with code {}",exitcode);
            System.exit(exitcode);
        }

    }
    public ConfigurableApplicationContext run(String[] args, Class<?> aClass) throws TaskConfigurationException, TaskFailedException {

        if (args.length >= 1) {

            ConfigurableApplicationContext applicationContext = SpringApplication.run(aClass, args);
            ScheduledTaskDTO scheduledTaskDTO = getAndValidateScheduledTaskDTO(applicationContext);

            logger.info("Try to start body");
            ProcessorBody body = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
            body.run(scheduledTaskDTO);
            return applicationContext;
        } else {
            String msg = "No one attribute found. Task configuration is null. Exit";
            logger.info(msg);
            throw new TaskConfigurationException(msg);
        }
    }
    private ScheduledTaskDTO getAndValidateScheduledTaskDTO (ConfigurableApplicationContext applicationContext) throws TaskConfigurationException {
        String scheduledTaskId = applicationContext.getEnvironment().getProperty("scheduledTaskId");
        logger.info("Received scheduledTaskId = {}. Requesting full body" , scheduledTaskId);
        SchedulerRestClientApi schedulerRestClientApi = applicationContext.getBean(SchedulerRestClientApi.class);
        ScheduledTaskDTO result = schedulerRestClientApi.getScheduledTaskDTO(scheduledTaskId);
        logger.info("Received scheduledTask with full name={}", result.buildTaskFullName());
        logger.info("Validate task configuration");
        ValidationResult validationResult = ScheduledTaskDTOValidator.validate(result);
        if (!validationResult.isValid())
            throw new TaskConfigurationException(validationResult.getDescriptions().stream().collect(Collectors.joining("\n")));
        if(result.getTaskProcessorBody() ==null ||result.getTaskProcessorBody().isBlank())
            throw new TaskConfigurationException("Value of taskProcessorBody must not be null");

        return result;
    }
/*

    public  <T> T  jsonToConf(String json, Class<T> clazz) throws TaskConfigurationException {

        try {
            logger.debug("Build {} from JSON\n{}",clazz.getSimpleName(),json);
            return  ObjectMapping.stringToObject(json, clazz);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }
*/

    public enum ExitCode {
        TaskConfigurationException(10001),
        TaskFailedException(10002),
        other(1),
        success(0);

        private final int value;
        ExitCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
