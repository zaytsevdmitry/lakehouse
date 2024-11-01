package lakehouse.taskexecutor.processor;

import org.lakehouse.api.rest.client.service.ClientApi;

import lakehouse.api.dto.tasks.ScheduledTaskDTO;


public class JdbcScriptExecProcessor extends AbstractProcessor{

	public JdbcScriptExecProcessor(
			ScheduledTaskDTO scheduledTaskDTO,
			ClientApi clientApi) {
		super(scheduledTaskDTO, clientApi);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
