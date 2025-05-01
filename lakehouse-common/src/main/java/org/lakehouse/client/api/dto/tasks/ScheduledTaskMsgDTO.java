package org.lakehouse.client.api.dto.tasks;

public class ScheduledTaskMsgDTO  {
	private Long id;
	private String taskExecutionServiceGroupName;

	public ScheduledTaskMsgDTO() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTaskExecutionServiceGroupName() {
		return taskExecutionServiceGroupName;
	}

	public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupName) {
		this.taskExecutionServiceGroupName = taskExecutionServiceGroupName;
	}
}
