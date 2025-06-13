package org.lakehouse.taskexecutor.service;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.MaintenanceAbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


@Service
public class ProcessorFactory {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfigFactory taskProcessorConfigFactory;
	private final StateRestClientApi stateRestClientApi;
	private final Jinjava jinjava;
    public ProcessorFactory(
            TaskProcessorConfigFactory taskProcessorConfigFactory,
            StateRestClientApi stateRestClientApi,
			Jinjava jinjava) {
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;

        this.stateRestClientApi = stateRestClientApi;
        this.jinjava = jinjava;
    }

	private TaskProcessor buildCustomProcessor(String executionModule, TaskProcessorConfig taskProcessorConfig)
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
		Constructor<?> constructor =
				processorClass.getConstructor(TaskProcessorConfig.class);


		logger.info("Making  processor class");
		return  (TaskProcessor) constructor.newInstance(taskProcessorConfig);


	}
	private TaskProcessor getMantenanceTaskProcessor(String executionModule, TaskProcessorConfig taskProcessorConfig){
		return new MaintenanceAbstractTaskProcessor(taskProcessorConfig, stateRestClientApi,jinjava) {
			@Override
			public Status.Task runTask() {
				return null;
			}
		};
	}
    public TaskProcessor buildProcessor(ScheduledTaskLockDTO scheduledTaskLockDTO)
			throws InstantiationException,
			IllegalAccessException,
			ClassNotFoundException,
			NoSuchMethodException,
			IllegalArgumentException,
			InvocationTargetException {
		logger.info("Get class for name");
		Class<?> processorClass = Class.forName(
				scheduledTaskLockDTO
						.getScheduledTaskEffectiveDTO()
						.getExecutionModule());
		logger.info("Loaded class:{}", processorClass.getName());
		logger.info("Define constructor");
		Constructor<?> constructor = 
				processorClass.getConstructor(TaskProcessorConfig.class, Jinjava.class);

		logger.info("Build config");
		TaskProcessorConfig tc = taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);
		logger.info("Config ready");

		logger.info("Making  processor class");
		return  (TaskProcessor) constructor.newInstance(tc, jinjava);
	}

}
