package org.lakehouse.taskexecutor.factory;

import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.common.api.task.processor.entity.TaskProcessor;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.lakehouse.taskexecutor.exception.TaskProcessorConfigurationException;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.AbstractSparkDeployTaskProcessor;
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
	private final SparkRestClientApi sparkRestClientApi;
	private final SparkConfigurationProperties sparkConfigurationProperties;
	public TaskProcessorFactory(
			StateRestClientApi stateRestClientApi,
			SparkRestClientApi sparkRestClientApi,
			SparkConfigurationProperties sparkConfigurationProperties) {
        this.stateRestClientApi = stateRestClientApi;
		this.sparkConfigurationProperties = sparkConfigurationProperties;
        this.sparkRestClientApi = sparkRestClientApi;
    }

	private TaskProcessor constructTaskProcessor(
			Class<?> processorClass,
			TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws  TaskProcessorConfigurationException {

		TaskProcessor result = null;

		try {

			Constructor<?> constructor = null;

			if ( AbstractStateTaskProcessor.class.isAssignableFrom(processorClass)) {
				logger.info("Making State maintenance class instance {}", processorClass.getName());
				constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class,  StateRestClientApi.class);
				result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO, stateRestClientApi);
			}
			else if ( AbstractDefaultTaskProcessor.class.isAssignableFrom(processorClass)) {
				logger.info("Making Default processor class instance {}", processorClass.getName());
				constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class);
				result =  (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO);
			}else if ( AbstractSparkDeployTaskProcessor.class.isAssignableFrom(processorClass)) {
				logger.info("Making Spark deployment processor class instance {}", processorClass.getName());
				constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class, SparkConfigurationProperties.class, SparkRestClientApi.class);
				result =  (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO, sparkConfigurationProperties, sparkRestClientApi);
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

    public TaskProcessor buildProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO, String executionModule)
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
		return constructTaskProcessor(processorClass, taskProcessorConfigDTO);

	}

}
