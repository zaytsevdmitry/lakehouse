package lakehouse.taskexecutor.processor;

import java.lang.reflect.Constructor;

import org.lakehouse.api.rest.client.service.ClientApi;

import lakehouse.api.dto.tasks.ScheduledTaskDTO;

public class ProcessorFactory {
	public Processor buildProcessor(ScheduledTaskDTO scheduledTaskDTO)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
		Class<?> processorClass = Class.forName(scheduledTaskDTO.getExecutionModule());
		Constructor<?> constructor = processorClass.getConstructor(ScheduledTaskDTO.class, ClientApi.class);
		return (Processor) processorClass.newInstance();
	}

}
