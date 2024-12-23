package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.constant.Status;
import org.lakehouse.cli.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.cli.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.cli.api.dto.service.TaskInstanceReleaseDTO;
@Component
public class TaskLockObjectActionsImpl implements TaskLockObjectActions{
	private final ClientApi clientApi;
	
	public TaskLockObjectActionsImpl(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getScheduledTaskLockDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduledTaskLockDTO> l = clientApi.getScheduledTaskLockDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{
						"lockId",
						"serviceId",
						"lastHeartBeatDateTime",
						"scheduleName",
						"scheduleTargetTimestamp",
						"scenarioActName",
						"name", 
						"status",
						"executionModule", 
						"taskExecutionServiceGroupName"}, 
				l.stream().map(o -> new String[]{
						o.getLockId().toString(),
						o.getServiceId(),
						o.getLastHeartBeatDateTime(),
						o.getScheduledTaskDTO().getScheduleName(),
						o.getScheduledTaskDTO().getScheduleTargetTimestamp(),
						o.getScheduledTaskDTO().getScenarioActName(),
						o.getScheduledTaskDTO().getName(), 
						o.getScheduledTaskDTO().getStatus(),
						o.getScheduledTaskDTO().getExecutionModule(),
						o.getScheduledTaskDTO().getTaskExecutionServiceGroupName()
				}).toList());
	}


	@Override
	public CommandResult lockNew(String[] args) {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.lockTask(args[2],args[3]));
	}
	@Override
	public CommandResult lockHeartBeat(String[] args) {
		TaskExecutionHeartBeatDTO h = new TaskExecutionHeartBeatDTO();
		h.setLockId(Long.valueOf( args[2]));
		return ObjectActionsHelper.coverHttpCode(clientApi.lockHeartBeat(h));
		
	}
		
	@Override
	public CommandResult lockRelease(String[] args) {
		TaskInstanceReleaseDTO r = new TaskInstanceReleaseDTO();
		r.setLockId(Long.valueOf(args[2].toUpperCase()));
		r.setStatus(Status.Task.valueOf(args[3].toUpperCase()).label);
		return ObjectActionsHelper.coverHttpCode(clientApi.lockRelease(r));
	}
}
