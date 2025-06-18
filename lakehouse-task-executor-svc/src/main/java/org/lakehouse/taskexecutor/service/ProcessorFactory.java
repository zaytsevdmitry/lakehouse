package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.DependencyCheckTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.SuccessTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


@Service
public class ProcessorFactory {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfigFactory taskProcessorConfigFactory;
	private final StateRestClientApi stateRestClientApi;
	private final Jinjava jinjava;
	private final Map<String,Class<?>> internalProcessors;
    public ProcessorFactory(
            TaskProcessorConfigFactory taskProcessorConfigFactory,
            StateRestClientApi stateRestClientApi,
			Jinjava jinjava) {
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;
        this.stateRestClientApi = stateRestClientApi;
        this.jinjava = jinjava;
		internalProcessors = new HashMap<>();
		internalProcessors.put(DependencyCheckTaskProcessor.class.getName(),DependencyCheckTaskProcessor.class);
		internalProcessors.put(SuccessTaskProcessor.class.getName(),SuccessTaskProcessor.class);
    }

	private TaskProcessor buildCustomProcessor(
			Class<?> processorClass,
			TaskProcessorConfig taskProcessorConfig)
			throws InstantiationException,
			IllegalAccessException,
			NoSuchMethodException,
			IllegalArgumentException,
			InvocationTargetException {

		Constructor<?> constructor =
				processorClass.getConstructor(TaskProcessorConfig.class, Jinjava.class);

		logger.info("Making custom processor class");
		return  (TaskProcessor) constructor.newInstance(taskProcessorConfig,jinjava);

	}

	private TaskProcessor buildMantenanceTaskProcessor(
			Class<?> processorClass,
			TaskProcessorConfig taskProcessorConfig)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		TaskProcessor result = null;

		if(
				DependencyCheckTaskProcessor.class.getName().equals(processorClass.getName())
				|| SuccessTaskProcessor.class.getName().equals(processorClass.getName())){
			Constructor<?> constructor =
					processorClass.getConstructor(TaskProcessorConfig.class,StateRestClientApi.class, Jinjava.class);
			logger.info("Making maintenance class instance {}", processorClass.getName());

			result = (TaskProcessor) constructor.newInstance(taskProcessorConfig, stateRestClientApi, jinjava);
		}

		return  result;
	}
    public TaskProcessor buildProcessor(TaskProcessorConfig taskProcessorConfig, String executionModule)
			throws InstantiationException,
			IllegalAccessException,
			ClassNotFoundException,
			NoSuchMethodException,
			IllegalArgumentException,
			InvocationTargetException {




		logger.info("Get class for name");
		Class<?> processorClass = Class.forName(executionModule);

		logger.info("Loaded class:{}", processorClass.getName());

		logger.info("Define constructor");
		if (internalProcessors.containsKey(processorClass.getName())){
			return buildMantenanceTaskProcessor(processorClass,taskProcessorConfig);
		}else {
			return buildCustomProcessor(processorClass,taskProcessorConfig);
		}

	}

}
