package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
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

            TaskProcessorConfigDTO taskProcessorConfigDTO = mapTaskProcessorConfigDTO(args[0]);
            SparkSession sparkSession = SparkSession.builder().getOrCreate();
            ProcessorBody body = ProcessorBodyFactory.build(
                    SparkProcessorBodyParamFactory.buildSparkProcessorBodyParameter(sparkSession, taskProcessorConfigDTO),
                    taskProcessorConfigDTO.getTaskProcessorBody());
            body.run();

        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new Exception(msg);
        }
    }
}
