package lakehouse.api.entities.tasks;

import jakarta.persistence.*;
import lakehouse.api.entities.configs.Schedule;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_instance_running_schedule_name_uk", columnNames = {
		"schedule_name" }))
public class ScheduleInstanceRunning {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@OneToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable = false)
	private Schedule schedule;

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

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public ScheduleInstance getScheduleInstance() {
		return scheduleInstance;
	}

	public void setScheduleInstance(ScheduleInstance scheduleInstance) {
		this.scheduleInstance = scheduleInstance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScheduleInstanceRunning that = (ScheduleInstanceRunning) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getSchedule(), that.getSchedule())
				&& Objects.equals(getScheduleInstance(), that.getScheduleInstance());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getSchedule(), getScheduleInstance());
	}
}
