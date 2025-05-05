package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_instance_running_schedule_name_uk", columnNames = {
		"config_schedule_key_name" }))
public class ScheduleInstanceRunning {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(nullable = false)
	private String configScheduleKeyName;

	@OneToOne
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private ScheduleInstance scheduleInstance;

	public ScheduleInstanceRunning() {
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

	public ScheduleInstance getScheduleInstance() {
		return scheduleInstance;
	}

	public void setScheduleInstance(ScheduleInstance scheduleInstance) {
		this.scheduleInstance = scheduleInstance;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ScheduleInstanceRunning that = (ScheduleInstanceRunning) o;
		return Objects.equals(getId(), that.getId())
				&& Objects.equals(getConfigScheduleKeyName(), that.getConfigScheduleKeyName())
				&& Objects.equals(getScheduleInstance(), that.getScheduleInstance());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getConfigScheduleKeyName(), getScheduleInstance());
	}
}
