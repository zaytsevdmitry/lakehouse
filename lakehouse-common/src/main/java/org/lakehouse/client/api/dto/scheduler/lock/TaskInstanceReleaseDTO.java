package org.lakehouse.client.api.dto.scheduler.lock;

public class TaskInstanceReleaseDTO {
	private Long lockId;
	private TaskResultDTO taskResult;
	public TaskInstanceReleaseDTO() {
	}
	
	public TaskInstanceReleaseDTO(Long lockId, TaskResultDTO taskResult) {
		this.lockId = lockId;
		this.taskResult = taskResult;
	}
	
	public Long getLockId() {
		return lockId;
	}
	public void setLockId(Long lockId) {
		this.lockId = lockId;
	}

	public TaskResultDTO getTaskResult() {
		return taskResult;
	}

	public void setTaskResult(TaskResultDTO taskResult) {
		this.taskResult = taskResult;
	}
}
