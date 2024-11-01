package lakehouse.api.entities.tasks;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
public class ScheduleScenarioActInstanceDependency {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne
	@JoinColumn(nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleScenarioActInstance from;

	@ManyToOne
	@JoinColumn(nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScheduleScenarioActInstance to;

	public ScheduleScenarioActInstanceDependency() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ScheduleScenarioActInstance getFrom() {
		return from;
	}

	public void setFrom(ScheduleScenarioActInstance from) {
		this.from = from;
	}

	public ScheduleScenarioActInstance getTo() {
		return to;
	}

	public void setTo(ScheduleScenarioActInstance to) {
		this.to = to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScheduleScenarioActInstanceDependency that = (ScheduleScenarioActInstanceDependency) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getFrom(), that.getFrom())
				&& Objects.equals(getTo(), that.getTo());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getFrom(), getTo());
	}
}
