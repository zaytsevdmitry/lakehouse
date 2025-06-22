package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.exception.TaskProcessorConfigurationException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.AbstractStateTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


@Service
public class TaskProcessorFactory {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final StateRestClientApi stateRestClientApi;
	private final Jinjava jinjava;
	public TaskProcessorFactory(
            StateRestClientApi stateRestClientApi,
			Jinjava jinjava) {
        this.stateRestClientApi = stateRestClientApi;
        this.jinjava = jinjava;
    }

	private TaskProcessor constructTaskProcessor(
			Class<?> processorClass,
			TaskProcessorConfig taskProcessorConfig)
            throws  TaskProcessorConfigurationException {

		TaskProcessor result = null;

		try {

			Constructor<?> constructor = null;

			if ( AbstractStateTaskProcessor.class.isAssignableFrom(processorClass)) {
				logger.info("Making State maintenance class instance {}", processorClass.getName());
				constructor = processorClass.getConstructor(TaskProcessorConfig.class, Jinjava.class, StateRestClientApi.class);
				result = (TaskProcessor) constructor.newInstance(taskProcessorConfig, jinjava,stateRestClientApi);
			}
			else if ( AbstractDefaultTaskProcessor.class.isAssignableFrom(processorClass)) {
				logger.info("Making Default processor class instance {}", processorClass.getName());
				constructor = processorClass.getConstructor(TaskProcessorConfig.class, Jinjava.class);
				result =  (TaskProcessor) constructor.newInstance(taskProcessorConfig,jinjava);
			}
			else
			{
				throw new TaskProcessorConfigurationException(
						String.format("Processor class found, but has unexpected type : class name  %s", processorClass.getName()));
			}

		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new TaskProcessorConfigurationException(String.format("Class '%s' constructor error", processorClass.getName()), e);
		}
		return  result;
	}

    public TaskProcessor buildProcessor(TaskProcessorConfig taskProcessorConfig, String executionModule)
            throws TaskProcessorConfigurationException {

		logger.info("Get class for name");
        Class<?> processorClass = null;
        try {
            processorClass = Class.forName(executionModule);
        } catch (ClassNotFoundException e) {
            throw new TaskProcessorConfigurationException("Class instantiate error",e);
        }

        logger.info("Loaded class:{}", processorClass.getName());
		logger.info("Define constructor");
		return constructTaskProcessor(processorClass,taskProcessorConfig);

	}

}
