package org.lakehouse.client.rest.scheduler;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;

import java.util.List;


public interface SchedulerRestClientApi{
	public ScheduledTaskMsgDTO getScheduledTaskDTO(String name) ;
	public ScheduledTaskLockDTO  getScheduledTaskLockDTO(String id);
	public int  deleteScheduledTaskDTO(String name);
	public int  postScheduledTaskDTO(ScheduledTaskMsgDTO o);
	public List<ScheduledTaskDTO> getScheduledTaskDTOList();
	public List<ScheduledTaskLockDTO>  getScheduledTaskLockDTOList();
	public ScheduledTaskLockDTO lockTaskById(Long taskId, String serviceId);
	public int lockHeartBeat (TaskExecutionHeartBeatDTO taskExecutionHeartBeat);
	public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) ;
}