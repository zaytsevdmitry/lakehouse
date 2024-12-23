package org.lakehouse.cli.api.dto.service;

public class TaskInstanceReleaseDTO {
	private Long lockId;
	private String status;
	public TaskInstanceReleaseDTO() {
	}
	
	public TaskInstanceReleaseDTO(Long lockId, String status) {
		this.lockId = lockId;
		this.status = status;
	}
	
	public Long getLockId() {
		return lockId;
	}
	public void setLockId(Long lockId) {
		this.lockId = lockId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
}
