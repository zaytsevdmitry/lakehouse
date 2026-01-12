package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.processor.body.sql.SQLProcessorBodyAbstract;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ProcessorBodyFactory {
    protected static  Class<?> getClassForName(String bodyClassName) throws TaskConfigurationException {
        if (bodyClassName == null || bodyClassName.isBlank())
            throw new TaskConfigurationException("Argument taskProcessorBody is empty");

        try {
            return Class.forName(bodyClassName);
        } catch (ClassNotFoundException e) {
            throw new TaskConfigurationException(e);
        }
    }
    protected static ProcessorBody buildInstance(
            Class<?> bodyClass,
            Class<? extends BodyParam> BodyParamClass,
            BodyParam bodyParam) throws TaskConfigurationException {
        if (SQLProcessorBodyAbstract.class.isAssignableFrom(bodyClass)) {
            Constructor<?> constructor = null;

            try {
                constructor = bodyClass.getConstructor(BodyParamClass);
            } catch (NoSuchMethodException e) {
                throw new TaskConfigurationException(String.format("Wrong constructor of %s", bodyClass.getCanonicalName()), e);
            }

            ProcessorBody result = null;

            try {
                result = (ProcessorBody) constructor.newInstance(bodyParam);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new TaskConfigurationException(String.format("Can't create instance of body %s", bodyClass.getCanonicalName()), e);
            }
            return result;
        } else {
            throw new TaskConfigurationException(String.format("Required %s type",SQLProcessorBodyAbstract.class.getCanonicalName()));
        }
    }
    public static ProcessorBody build(BodyParam bodyParam, String bodyClassName) throws TaskFailedException, TaskConfigurationException {

        return buildInstance(
                getClassForName(bodyClassName),
                BodyParam.class,
                bodyParam
        );


    }
}
