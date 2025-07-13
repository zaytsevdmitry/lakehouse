package org.lakehouse.client.api.dto.scheduler;

import java.util.Objects;

public class ScheduleInstanceDTO {
	private Long id;

	private String configScheduleKeyName;

	private String targetExecutionDateTime;

	private String status;

	public ScheduleInstanceDTO() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getConfigScheduleKeyName() {
		return configScheduleKeyName;
	}

	public void setConfigScheduleKeyName(String configScheduleKeyName) {
		this.configScheduleKeyName = configScheduleKeyName;
	}

	public String getTargetExecutionDateTime() {
		return targetExecutionDateTime;
	}

	public void setTargetExecutionDateTime(String targetExecutionDateTime) {
		this.targetExecutionDateTime = targetExecutionDateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScheduleInstanceDTO that = (ScheduleInstanceDTO) o;
		return Objects.equals(getId(), that.getId()) 
				&& Objects.equals(getConfigScheduleKeyName(), that.getConfigScheduleKeyName())
				&& Objects.equals(getTargetExecutionDateTime(), that.getTargetExecutionDateTime())
				&& Objects.equals(getStatus(), that.getStatus());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getConfigScheduleKeyName(), getTargetExecutionDateTime(), getStatus());
	}
}
