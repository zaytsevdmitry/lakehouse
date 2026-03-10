package org.lakehouse.taskexecutor.spark.dq;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.SparkProcessorBodyParamFactory;
import org.lakehouse.taskexecutor.spark.configuration.CatalogActivatorConfiguration;
import org.lakehouse.taskexecutor.spark.configuration.SparkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.api",
                "org.lakehouse.taskexecutor.spark.dq"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                JinJavaConfiguration.class,
                SparkConfiguration.class,
                CatalogActivatorConfiguration.class
        }
        )
public class SparkProcessorApplicationDQ {

    private final static Logger logger = LoggerFactory.getLogger(SparkProcessorApplicationDQ.class);

    public static <T> T  jsonToConf(String json, Class<T> clazz) throws TaskConfigurationException {

        try {
            logger.debug("Build {} from JSON\n{}",clazz.getSimpleName(),json);
            return  ObjectMapping.stringToObject(json, clazz);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }
    public static String[] adaptArgs(String[] args,ScheduledTaskDTO scheduledTaskDTO) throws TaskConfigurationException {
        logger.info(args[0]);


        String [] args2;

        Set<String> args2Set = new HashSet<>();

        if (args.length > 1) {
            String [] otherArgs = Arrays.copyOfRange(args, 1, args.length);

            args2Set.addAll(Arrays.asList(otherArgs));
        }
        args2Set.addAll(scheduledTaskDTO
                .getTaskProcessorArgs()
                .entrySet()
                .stream()
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .collect(Collectors.toSet()));

        args2Set.forEach(s -> logger.info("Spring Application parameters {}", s));

        return args2Set.toArray(new String[0]);
    }
    public static void main(String[] args) throws TaskConfigurationException, TaskFailedException {

        if (args.length >= 1) {
            ScheduledTaskDTO scheduledTaskDTO = jsonToConf(args[0],ScheduledTaskDTO.class);

            ConfigurableApplicationContext applicationContext = SpringApplication.run(SparkProcessorApplicationDQ.class, adaptArgs(args,scheduledTaskDTO));


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