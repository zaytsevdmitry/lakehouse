package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.Schedule;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "scenario_act_edge_uk", columnNames = { "schedule_name",
		"from_scenario_act_id", "to_scenario_act_id" }))
public class ScenarioActEdge {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_edge__schedule_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Schedule schedule;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_edge__from_scenario_act_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioAct fromScenarioAct;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_edge__to_scenario_act_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioAct toScenarioAct;

	public ScenarioActEdge() {
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

	public ScenarioAct getFromScenarioAct() {
		return fromScenarioAct;
	}

	public void setFromScenarioAct(ScenarioAct fromScenarioAct) {
		this.fromScenarioAct = fromScenarioAct;
	}

	public ScenarioAct getToScenarioAct() {
		return toScenarioAct;
	}

	public void setToScenarioAct(ScenarioAct toScenarioAct) {
		this.toScenarioAct = toScenarioAct;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScenarioActEdge that = (ScenarioActEdge) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getSchedule(), that.getSchedule())
				&& Objects.equals(getFromScenarioAct(), that.getFromScenarioAct())
				&& Objects.equals(getToScenarioAct(), that.getToScenarioAct());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getSchedule(), getFromScenarioAct(), getToScenarioAct());
	}
}
