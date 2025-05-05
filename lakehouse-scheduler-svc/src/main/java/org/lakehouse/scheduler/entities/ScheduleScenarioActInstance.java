package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
public class ScheduleScenarioActInstance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(nullable = false)
	private String name;

	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable = false)
	private ScheduleInstance scheduleInstance;

	@Column(nullable = false)
	private String confDataSetKeyName;

	@Column(nullable = false)
	private String status;

	public ScheduleScenarioActInstance() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ScheduleInstance getScheduleInstance() {
		return scheduleInstance;
	}

	public void setScheduleInstance(ScheduleInstance scheduleInstance) {
		this.scheduleInstance = scheduleInstance;
	}

	public String getConfDataSetKeyName() {
		return confDataSetKeyName;
	}

	public void setConfDataSetKeyName(String confDataSetKeyName) {
		this.confDataSetKeyName = confDataSetKeyName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScheduleScenarioActInstance that = (ScheduleScenarioActInstance) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName())
				&& Objects.equals(getScheduleInstance(), that.getScheduleInstance())
				&& Objects.equals(getConfDataSetKeyName(), that.getConfDataSetKeyName()) && Objects.equals(getStatus(), that.getStatus());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getName(), getScheduleInstance(), getConfDataSetKeyName(), getStatus());
	}
}
