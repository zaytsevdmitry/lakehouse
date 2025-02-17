package org.lakehouse.client.rest.scheduler;

import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;

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
	public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO);
}