package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;

public class SparkProcessorBodyFactory {

    public ProcessorBody build(SparkBodyParam bodyParam , String bodyClassName) throws TaskFailedException, TaskConfigurationException {
        Class<?> bodyClass = ProcessorBodyFactory.getClassForName(bodyClassName);
        if (SparkProcessorBodyAbstract.class.isAssignableFrom(bodyClass)){
            return ProcessorBodyFactory.buildInstance(bodyClass,SparkBodyParam.class,bodyParam);
        }else{
            return ProcessorBodyFactory.buildInstance(bodyClass,BodyParam.class,(BodyParam) bodyParam);
        }
    }
}
