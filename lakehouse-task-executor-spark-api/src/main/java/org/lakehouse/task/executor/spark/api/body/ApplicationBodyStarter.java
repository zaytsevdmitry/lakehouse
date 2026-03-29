package org.lakehouse.task.executor.spark.api.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
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

    public  <T> T  jsonToConf(String json, Class<T> clazz) throws TaskConfigurationException {

        try {
            logger.debug("Build {} from JSON\n{}",clazz.getSimpleName(),json);
            return  ObjectMapping.stringToObject(json, clazz);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }

    public void runAndStop(String[] args, Class<?> aClass) throws InterruptedException {
        ConfigurableApplicationContext context = null;
        int exitcode = ExitCode.success.getValue();
        try {
            logger.info("Create context");
            context = run(args,aClass);

        } catch (TaskConfigurationException e) {
            logger.info(Arrays.toString(e.getStackTrace()));
            exitcode = ExitCode.TaskConfigurationException.getValue();

        } catch (TaskFailedException e) {
            logger.info(Arrays.toString(e.getStackTrace()));
            exitcode = ExitCode.TaskFailedException.getValue();
        } catch(Exception e) {
            logger.info(Arrays.toString(e.getStackTrace()));
            exitcode = ExitCode.other.getValue();
        } finally {
            if (context!=null) {
                SparkSession sparkSession = context.getBean(SparkSession.class);
                logger.info("Stopping Spark session");
                sparkSession.stop();
                Thread.sleep(40000L); //todo made app parameter
                logger.info("Stopping context");
                context.stop();
                Thread.sleep(40000L); //todo made app parameter
            }
            logger.info("Exit application");
            System.exit(exitcode);
        }

    }
    public ConfigurableApplicationContext run(String[] args, Class<?> aClass) throws TaskConfigurationException, TaskFailedException {

        if (args.length >= 1) {
            logger.info(args[0]);
            ScheduledTaskDTO scheduledTaskDTO = jsonToConf(args[0],ScheduledTaskDTO.class);
            logger.info("Validate task configuration");
            ValidationResult validationResult = ScheduledTaskDTOValidator.validate(scheduledTaskDTO);
            if (!validationResult.isValid())
                throw new TaskConfigurationException(validationResult.getDescriptions().stream().collect(Collectors.joining("\n")));
            if(scheduledTaskDTO.getTaskProcessorBody() ==null ||scheduledTaskDTO.getTaskProcessorBody().isBlank())
                throw new TaskConfigurationException("Value of taskProcessorBody must not be null");

            String [] args2;

            if (args.length > 1) {
                args2 = Arrays.copyOfRange(args, 1, args.length);
                for(String a:args2)
                    logger.info("Spring Application parameters {}", a);
            }else {
                args2 = new String[]{};
            }

            ConfigurableApplicationContext applicationContext = SpringApplication.run(aClass, args);


            ProcessorBody body = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
            body.run(scheduledTaskDTO);
            return applicationContext;
        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new TaskConfigurationException(msg);
        }
    }
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
