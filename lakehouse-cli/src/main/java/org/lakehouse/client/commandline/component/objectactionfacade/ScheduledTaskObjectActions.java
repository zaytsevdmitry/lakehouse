package org.lakehouse.client.commandline.component.objectactionfacade;

import java.util.List;

import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTaskObjectActions implements ObjectActions{
	private final SchedulerRestClientApi schedulerRestClientApi;
	
	public ScheduledTaskObjectActions(SchedulerRestClientApi schedulerRestClientApi) {
		this.schedulerRestClientApi = schedulerRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( schedulerRestClientApi.getScheduledTaskDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduledTaskDTO> l = schedulerRestClientApi.getScheduledTaskDTOList();
		
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
