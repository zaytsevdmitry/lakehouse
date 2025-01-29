package org.lakehouse.client.commandline.component.objectactionfacade;

import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.component.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
@Component
public class ScheduledTaskObjectActions implements ObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public ScheduledTaskObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getScheduledTaskDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduledTaskDTO> l = configRestClientApi.getScheduledTaskDTOList();
		
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
