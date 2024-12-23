package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_instance_uk_name_targetdt_uk", columnNames = {
		"schedule_name", "target_execution_date_time" }))
public class ScheduleInstance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(nullable = false)
	private String configScheduleKeyName;

	@Column(nullable = false)
	private OffsetDateTime targetExecutionDateTime;

	@Column(nullable = false)
	private String status;

	public ScheduleInstance() {
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

	public OffsetDateTime getTargetExecutionDateTime() {
		return targetExecutionDateTime;
	}

	public void setTargetExecutionDateTime(OffsetDateTime targetExecutionDateTime) {
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
		ScheduleInstance that = (ScheduleInstance) o;
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
