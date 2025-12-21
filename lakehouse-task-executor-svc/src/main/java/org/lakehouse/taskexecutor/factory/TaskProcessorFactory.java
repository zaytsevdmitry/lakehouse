package org.lakehouse.taskexecutor.factory;

import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.exception.TaskProcessorConfigurationException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.AbstractSparkDeployTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.AbstractStateTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


@Service
public class TaskProcessorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StateRestClientApi stateRestClientApi;
    public TaskProcessorFactory(
            StateRestClientApi stateRestClientApi) {
        this.stateRestClientApi = stateRestClientApi;
    }

    private TaskProcessor constructTaskProcessor(
            Class<?> processorClass,
            TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws TaskProcessorConfigurationException {

        TaskProcessor result = null;

        try {

            Constructor<?> constructor = null;

            if (AbstractStateTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making State maintenance class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class, StateRestClientApi.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO, stateRestClientApi);
            } else if (AbstractDefaultTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making Default processor class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO);
            } else if (AbstractSparkDeployTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making Spark deployment processor class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO);
            } else {
                throw new TaskProcessorConfigurationException(
                        String.format("Processor class found, but has unexpected type : class name  %s", processorClass.getName()));
            }

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new TaskProcessorConfigurationException(String.format("Class '%s' constructor error", processorClass.getName()), e);
        }
        return result;
    }

    public TaskProcessor buildProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO, String executionModule)
            throws TaskProcessorConfigurationException {

        Class<?> processorClass = null;
        logger.info("Try load class:{}", executionModule);
        try {
            processorClass = Class.forName(executionModule);
        } catch (ClassNotFoundException e) {
            throw new TaskProcessorConfigurationException("Class instantiate error", e);
        }

        logger.info("Loaded class:{}", processorClass.getName());
        logger.info("Define constructor");
        return constructTaskProcessor(processorClass, taskProcessorConfigDTO);

    }

}
