package org.lakehouse.taskexecutor.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessorFactory {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfigFactory taskProcessorConfigFactory;
    public ProcessorFactory(
			TaskProcessorConfigFactory taskProcessorConfigFactory) {
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;

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
