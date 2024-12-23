package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.tasks.ScheduledTaskDTO;
@Component
public class ScheduledTaskObjectActions implements ObjectActions{
	private final ClientApi clientApi;
	
	public ScheduledTaskObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getScheduledTaskDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduledTaskDTO> l = clientApi.getScheduledTaskDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{
						"scheduleName",
						"scheduleTargetTimestamp",
						"scenarioActName",
						"name", 
						"status",
						"executionModule", 
						"taskExecutionServiceGroupName"}, 
				l.stream().map(o -> new String[]{
						o.getScheduleName(),
						o.getScheduleTargetTimestamp(),
						o.getScenarioActName(),
						o.getName(), 
						o.getStatus(),
						o.getExecutionModule(),
						o.getTaskExecutionServiceGroupName()
				}).toList());
	}

}
