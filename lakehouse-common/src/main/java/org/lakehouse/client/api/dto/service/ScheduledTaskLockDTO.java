package org.lakehouse.client.api.dto.service;

import org.lakehouse.client.api.dto.configs.TaskDTO;

public class ScheduledTaskLockDTO {
	private Long lockId;
	private TaskDTO scheduledTaskEffectiveDTO;
	private String lastHeartBeatDateTime;
	private String serviceId;
	private String scheduleConfKeyName;
	private String scenarioActConfKeyName;
	private String scheduleTargetDateTime;
	private String dataSetKeyName;
	public void setDataSetKeyName(String dataSetKeyName) {
		this.dataSetKeyName = dataSetKeyName;
	}

	public String getDataSetKeyName() {
		return dataSetKeyName;
	}
	public ScheduledTaskLockDTO() {
		
	}
	
	public Long getLockId() {
		return lockId;
	}
	
	public void setLockId(Long lockId) {
		this.lockId = lockId;
	}

	public TaskDTO getScheduledTaskEffectiveDTO() {
		return scheduledTaskEffectiveDTO;
	}

	public void setScheduledTaskEffectiveDTO(TaskDTO scheduledTaskEffectiveDTO) {
		this.scheduledTaskEffectiveDTO = scheduledTaskEffectiveDTO;
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

	public String getScheduleConfKeyName() {
		return scheduleConfKeyName;
	}

	public void setScheduleConfKeyName(String scheduleConfKeyName) {
		this.scheduleConfKeyName = scheduleConfKeyName;
	}

	public String getScenarioActConfKeyName() {
		return scenarioActConfKeyName;
	}

	public void setScenarioActConfKeyName(String scenarioActConfKeyName) {
		this.scenarioActConfKeyName = scenarioActConfKeyName;
	}

	public String getScheduleTargetDateTime() {
		return scheduleTargetDateTime;
	}

	public void setScheduleTargetDateTime(String scheduleTargetDateTime) {
		this.scheduleTargetDateTime = scheduleTargetDateTime;
	}
}
