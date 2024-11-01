package lakehouse.taskexecutor.processor;
import org.lakehouse.api.rest.client.service.ClientApi;

import lakehouse.api.dto.tasks.ScheduledTaskDTO;
public abstract class AbstractProcessor implements Processor{

	private final ScheduledTaskDTO scheduledTaskDTO;
	private final ClientApi clientApi;
	public AbstractProcessor(ScheduledTaskDTO scheduledTaskDTO, ClientApi clientApi) {
		this.scheduledTaskDTO = scheduledTaskDTO;
		this.clientApi = clientApi;

	}
		

}
