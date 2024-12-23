package org.lakehouse.cli.api.dto.service;

import org.lakehouse.cli.api.dto.tasks.ScheduledTaskDTO;

public class ScheduledTaskLockDTO {
	private Long lockId;
	private ScheduledTaskDTO scheduledTaskDTO;
	private String lastHeartBeatDateTime;
	private String serviceId;
	public ScheduledTaskLockDTO() {
		
	}
	
	public Long getLockId() {
		return lockId;
	}
	
	public void setLockId(Long lockId) {
		this.lockId = lockId;
	}
	
	public ScheduledTaskDTO getScheduledTaskDTO() {
		return scheduledTaskDTO;
	}
	
	public void setScheduledTaskDTO(ScheduledTaskDTO scheduledTaskDTO) {
		this.scheduledTaskDTO = scheduledTaskDTO;
	}

	public String getLastHeartBeatDateTime() {
		return lastHeartBeatDateTime;
	}

	public void setLastHeartBeatDateTime(String lastHeartBeatDateTime) {
		this.lastHeartBeatDateTime = lastHeartBeatDateTime;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
}
