package org.lakehouse.client.rest.scheduler;

import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;

import java.util.List;


public interface SchedulerRestClientApi{
	public ScheduledTaskDTO  getScheduledTaskDTO(String name) ;
	public ScheduledTaskLockDTO  getScheduledTaskLockDTO(String id);
	public int  deleteScheduledTaskDTO(String name);
	public int  postScheduledTaskDTO(ScheduledTaskDTO o);

	public List<ScheduledTaskDTO> getScheduledTaskDTOList();
	public List<ScheduledTaskLockDTO>  getScheduledTaskLockDTOList();
	public ScheduledTaskLockDTO lockTask(String taskExecutionServiceGroupName, String serviceId);
	public int lockHeartBeat (TaskExecutionHeartBeatDTO taskExecutionHeartBeat);
	public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) ;





}