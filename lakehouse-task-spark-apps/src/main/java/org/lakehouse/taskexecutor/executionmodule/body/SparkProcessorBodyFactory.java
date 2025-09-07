package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public  class SparkProcessorBodyFactory {


    public static SparkProcessorBody buildSparkProcessorBody(
            SparkSession sparkSession,
            String[] args)
            throws TaskFailedException {
        TaskProcessorConfigDTO taskProcessorConfigDTO = null;
        try {
            taskProcessorConfigDTO = ObjectMapping.stringToObject(args[0], TaskProcessorConfigDTO.class);
        } catch (IOException e) {
            throw new TaskFailedException(e);
        }
        String executionBodyName = taskProcessorConfigDTO.getExecutionModuleArgs().get("executionBody");
        if (executionBodyName == null || executionBodyName.isBlank())
            throw new TaskFailedException("Argument 'executionBody' is empty");
        String[] leastArgs;
        if(args.length > 1) {
            leastArgs = new String[args.length - 1];
            for (int i = 1; i <= args.length; i++)
                leastArgs[i] = args[i];
        }
        else leastArgs = new String[0];
        BodyParam bodyParam = new BodyParam(sparkSession,taskProcessorConfigDTO,leastArgs);

        Class<?> processorBodyClass = null;
        try {
            processorBodyClass = Class.forName(executionBodyName);
        } catch (ClassNotFoundException e) {
            throw new TaskFailedException(e.getMessage(),e);
        }

        if ( SparkProcessorBodyAbstract.class.isAssignableFrom(processorBodyClass)) {
            Constructor<?> constructor = null;
            try {
                constructor = processorBodyClass.getConstructor(bodyParam.getClass());
            } catch (NoSuchMethodException e) {
                throw new TaskFailedException(String.format("Wrong constructor of %s", executionBodyName),e);
            }
            SparkProcessorBody result = null;
            try {
                result = (SparkProcessorBody) constructor.newInstance(bodyParam);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new TaskFailedException(String.format("Can't create instance of body %s", executionBodyName),e);
            }
            return result;
        }else {
            throw new TaskFailedException("Its not a SparkProcessorBodyAbstract type");
        }
    }
}
