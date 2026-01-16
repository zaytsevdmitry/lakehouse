package org.lakehouse.taskexecutor.executionmodule.body;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBodyFactory;
import org.lakehouse.taskexecutor.api.processor.body.SparkProcessorBodyParamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class  SparkProcessorBodyStarter {
    private final static Logger logger = LoggerFactory.getLogger(SparkProcessorBodyStarter.class);
    public static TaskProcessorConfigDTO mapTaskProcessorConfigDTO(String json) throws TaskFailedException {

        try {
            logger.debug("Build TaskProcessorConfigDTO from JSON\n{}",json);
            return  ObjectMapping.stringToObject(json, TaskProcessorConfigDTO.class);
        } catch (IOException e) {
            throw new TaskFailedException(e);
        }
    }
    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(SparkProcessorBodyStarter.class);
        if (args.length >= 1) {
            logger.info(args[0]);
            TaskProcessorConfigDTO taskProcessorConfigDTO = mapTaskProcessorConfigDTO(args[0]);
            Jinjava jinjava = JinJavaFactory.getJinjava(taskProcessorConfigDTO);

            SparkSession sparkSession = SparkSession.builder().getOrCreate();


            CatalogActivator catalogActivator = new CatalogActivator(sparkSession,jinjava);
            catalogActivator.activate(
                    taskProcessorConfigDTO.getDataSources().values().stream().toList(),
                    taskProcessorConfigDTO.getDrivers()
            );


            ProcessorBody body = ProcessorBodyFactory.build(
                    SparkProcessorBodyParamFactory.buildSparkProcessorBodyParameter(sparkSession, taskProcessorConfigDTO,jinjava),
                    taskProcessorConfigDTO.getTaskProcessorBody());
            body.run();

        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new Exception(msg);
        }
    }
}
