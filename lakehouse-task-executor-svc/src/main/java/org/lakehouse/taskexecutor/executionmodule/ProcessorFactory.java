package org.lakehouse.taskexecutor.executionmodule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;

public class ProcessorFactory {
	public TaskProcessor buildProcessor(ScheduledTaskDTO scheduledTaskDTO)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class<?> processorClass = Class.forName(scheduledTaskDTO.getExecutionModule());
		Constructor<?> constructor = 
				processorClass.getConstructor(Map.class);
		return (TaskProcessor) constructor
					.newInstance(scheduledTaskDTO.getExecutionModuleArgs());
	}

}
