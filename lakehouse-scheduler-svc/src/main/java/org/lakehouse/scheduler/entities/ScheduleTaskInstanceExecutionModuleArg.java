package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "schedule_task_instance_execution_lock_schedule_task_instance_id__key_uk", 
				columnNames = {"schedule_task_instance_id" , "key" }),
	}
)
public class ScheduleTaskInstanceExecutionModuleArg extends KeyValueAbstract {

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "schedule_task_instance_execution_module_arg__schedule_task_instance_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleTaskInstance scheduleTaskInstance;

	public ScheduleTaskInstanceExecutionModuleArg() {
	}

	public ScheduleTaskInstance getScheduleTaskInstance() {
		return scheduleTaskInstance;
	}

	public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
		this.scheduleTaskInstance = scheduleTaskInstance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		ScheduleTaskInstanceExecutionModuleArg that = (ScheduleTaskInstanceExecutionModuleArg) o;
		return Objects.equals(getScheduleTaskInstance(), that.getScheduleTaskInstance());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getScheduleTaskInstance());
	}
}
