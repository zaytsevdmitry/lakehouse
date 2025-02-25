package org.lakehouse.taskexecutor.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.springframework.stereotype.Service;

@Service
public class ProcessorFactory {
	private final TaskProcessorConfigFactory taskProcessorConfigFactory;

    public ProcessorFactory(TaskProcessorConfigFactory taskProcessorConfigFactory) {
        this.taskProcessorConfigFactory = taskProcessorConfigFactory;
    }

    public TaskProcessor buildProcessor(ScheduledTaskLockDTO scheduledTaskLockDTO)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class<?> processorClass = Class.forName(
				scheduledTaskLockDTO
						.getScheduledTaskEffectiveDTO()
						.getExecutionModule());
		Constructor<?> constructor = 
				processorClass.getConstructor(TaskProcessorConfig.class);
		return (TaskProcessor) constructor
					.newInstance(taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO));
	}

}
