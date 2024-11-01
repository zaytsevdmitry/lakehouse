package lakehouse.api.entities.tasks;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_task_instance_dependency_schedule_task_instance_id_depends_id_uk", columnNames = {
		"schedule_task_instance_id", "depends_id" }))
public class ScheduleTaskInstanceDependency {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne
	@JoinColumn(nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleTaskInstance scheduleTaskInstance;

	@ManyToOne
	@JoinColumn(nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleTaskInstance depends;

	@Column(nullable = false/* , columnDefinition = "boolean default false)" */)
	private boolean satisfied = false;

	public ScheduleTaskInstanceDependency() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ScheduleTaskInstance getScheduleTaskInstance() {
		return scheduleTaskInstance;
	}

	public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
		this.scheduleTaskInstance = scheduleTaskInstance;
	}

	public ScheduleTaskInstance getDepends() {
		return depends;
	}

	public void setDepends(ScheduleTaskInstance depends) {
		this.depends = depends;
	}

	public boolean isSatisfied() {
		return satisfied;
	}

	public void setSatisfied(boolean satisfied) {
		this.satisfied = satisfied;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScheduleTaskInstanceDependency that = (ScheduleTaskInstanceDependency) o;
		return isSatisfied() == that.isSatisfied() && Objects.equals(getId(), that.getId())
				&& Objects.equals(getScheduleTaskInstance(), that.getScheduleTaskInstance())
				&& Objects.equals(getDepends(), that.getDepends());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getScheduleTaskInstance(), getDepends(), isSatisfied());
	}
}
