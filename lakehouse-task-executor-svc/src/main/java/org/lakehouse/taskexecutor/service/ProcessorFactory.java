package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.MaintenanceAbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

@Service
public class ProcessorFactory {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfigFactory taskProcessorConfigFactory;

    public ProcessorFactory(
            TaskProcessorConfigFactory taskProcessorConfigFactory) {
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;

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
		return new MaintenanceAbstractTaskProcessor(taskProcessorConfig, new HashMap<>()) {
			@Override
			public Status.Task runTask() {
				return null;
			}
		};
	}
    public TaskProcessor buildProcessor(ScheduledTaskLockDTO scheduledTaskLockDTO)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		logger.info("Get class for name");
		Class<?> processorClass = Class.forName(
				scheduledTaskLockDTO
						.getScheduledTaskEffectiveDTO()
						.getExecutionModule());
		logger.info("Loaded class:{}", processorClass.getName());
		logger.info("Define constructor");
		Constructor<?> constructor = 
				processorClass.getConstructor(TaskProcessorConfig.class);

		logger.info("Build config");
		TaskProcessorConfig tc = taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);
		logger.info("Config ready");

		logger.info("Making  processor class");
		return  (TaskProcessor) constructor.newInstance(tc);

	}

}
