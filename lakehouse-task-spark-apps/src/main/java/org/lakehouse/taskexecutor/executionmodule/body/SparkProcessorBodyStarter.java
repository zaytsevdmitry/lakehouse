package org.lakehouse.taskexecutor.executionmodule.body;

import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.client.rest.taskexecutor.TaskExecutorRestClientApiImpl;
import org.lakehouse.common.api.task.processor.entity.TaskProcessor;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class SparkProcessorBodyStarter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(SparkProcessorBodyStarter.class);
        if( args.length >= 1){

                TaskProcessorConfigDTO taskProcessorConfigDTO =
                ObjectMapping.stringToObject(args[0], TaskProcessorConfigDTO.class);

                String executionBodyName = taskProcessorConfigDTO.getExecutionModuleArgs().get("executionBody");
                Class<?> processorBodyClass = null;
                    processorBodyClass = Class.forName(executionBodyName);
                    if ( SparkProcessorBodyAbstract.class.isAssignableFrom(processorBodyClass)) {
                        Constructor<?> constructor = processorBodyClass.getConstructor(TaskProcessorConfigDTO.class);
                        SparkProcessorBody result = (SparkProcessorBody) constructor.newInstance(taskProcessorConfigDTO);
                        result.run();
                    }else {
                        throw new TaskFailedException("Its not a SparkProcessorBodyAbstract type");
                    }

        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new Exception(msg);
        }
    }
}
