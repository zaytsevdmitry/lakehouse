package org.lakehouse.taskexecutor.spark;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.SparkProcessorBodyParamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.api",
                "org.lakehouse.taskexecutor.spark"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                JinJavaConfiguration.class
        })
public class SparkProcessorApplication {

    private final static Logger logger = LoggerFactory.getLogger(SparkProcessorApplication.class);

    public static <T> T  jsonToConf(String json, Class<T> clazz) throws TaskConfigurationException {

        try {
            logger.debug("Build {} from JSON\n{}",clazz.getSimpleName(),json);
            return  ObjectMapping.stringToObject(json, clazz);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }
    public static void main(String[] args) throws TaskConfigurationException, TaskFailedException {
        if (args.length >= 1) {
            logger.info(args[0]);
            ScheduledTaskDTO scheduledTaskDTO = jsonToConf(args[0],ScheduledTaskDTO.class);

            String [] args2;

            if (args.length > 1) {
                args2 = Arrays.copyOfRange(args, 1, args.length);
                for(String a:args2)
                    logger.info("Spring Application parameters {}", a);
            }else {
                args2 = new String[]{};
            }
            ConfigurableApplicationContext applicationContext = SpringApplication.run(SparkProcessorApplication.class, args2);

            SparkSession sparkSession = SparkSession.builder().getOrCreate();
            sparkSession.log().info("Start application");

            SparkProcessorBodyParamFactory sparkProcessorBodyParamFactory = applicationContext.getBean(SparkProcessorBodyParamFactory.class);
            ProcessorBody body = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
            BodyParam bodyParam = sparkProcessorBodyParamFactory.buildSparkProcessorBodyParameter(scheduledTaskDTO);
            body.run(bodyParam);
            SpringApplication.exit(applicationContext);
        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new TaskFailedException(msg);
        }
    }
}