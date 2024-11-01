package lakehouse.api.entities.tasks;

import jakarta.persistence.*;
import lakehouse.api.entities.configs.Schedule;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

	@ManyToOne
	@JoinColumn(nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Schedule schedule;

	@Column(nullable = false)
	private OffsetDateTime targetExecutionDateTime;

	@Column(nullable = false, columnDefinition = "varchar default 'NEW' CHECK (status IN ('NEW', 'READY', 'RUNNING', 'SUCCESS'))")
	private String status = "NEW";

	public ScheduleInstance() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
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
		return Objects.equals(getId(), that.getId()) && Objects.equals(getSchedule(), that.getSchedule())
				&& Objects.equals(getTargetExecutionDateTime(), that.getTargetExecutionDateTime())
				&& Objects.equals(getStatus(), that.getStatus());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getSchedule(), getTargetExecutionDateTime(), getStatus());
	}
}
