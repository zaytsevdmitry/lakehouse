package org.lakehouse.taskexecutor.executionmodule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;

public class ProcessorFactory {
	public TaskProcessor buildProcessor(TaskDTO taskDTO)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class<?> processorClass = Class.forName(taskDTO.getExecutionModule());
		Constructor<?> constructor = 
				processorClass.getConstructor(Map.class);
		return (TaskProcessor) constructor
					.newInstance(taskDTO.getExecutionModuleArgs());
	}

}
