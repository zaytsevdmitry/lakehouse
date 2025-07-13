package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Entity
@Table(uniqueConstraints = { 
		@UniqueConstraint(name = "schedule_task_instance_execution_lock_id_uk", columnNames = {"id" }),
		@UniqueConstraint(name = "schedule_task_instance_execution_lock_schedule_task_instance_id_uk", columnNames = {"schedule_task_instance_id" }),
	}
)
public class ScheduleTaskInstanceExecutionLock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column( nullable = false)
	private String serviceId;
	
	@Column( nullable = false)
	private OffsetDateTime lastHeartBeatDateTime;
	
	@ManyToOne
	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "schedule_task_instance_execution_lock__sti_id_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleTaskInstance scheduleTaskInstance;
	
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public ScheduleTaskInstance getScheduleTaskInstance() {
		return scheduleTaskInstance;
	}
	public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
		this.scheduleTaskInstance = scheduleTaskInstance;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public OffsetDateTime getLastHeartBeatDateTime() {
		return lastHeartBeatDateTime;
	}
	public void setLastHeartBeatDateTime(OffsetDateTime lastHeartBeatDateTime) {
		this.lastHeartBeatDateTime = lastHeartBeatDateTime;
	}

	@Override
	public String toString() {
		return "ScheduleTaskInstanceExecutionLock{" +
				"id=" + id +
				", serviceId='" + serviceId + '\'' +
				", lastHeartBeatDateTime=" + lastHeartBeatDateTime +
				", scheduleTaskInstance=" + scheduleTaskInstance +
				'}';
	}
}
