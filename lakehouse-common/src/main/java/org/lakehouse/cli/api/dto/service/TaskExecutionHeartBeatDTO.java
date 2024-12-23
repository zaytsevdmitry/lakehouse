package org.lakehouse.cli.api.dto.service;

public class TaskExecutionHeartBeatDTO {

	private Long lockId;
	public Long getLockId() {
		return lockId;
	}
	public void setLockId(Long lockId) {
		this.lockId = lockId;
	}
}
