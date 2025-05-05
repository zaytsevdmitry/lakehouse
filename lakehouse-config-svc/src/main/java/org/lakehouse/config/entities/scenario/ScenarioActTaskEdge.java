package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
  @Table(uniqueConstraints = {
		  @UniqueConstraint(
				  name = "scenario_act_task_edge__scenario_act_id__from_scenario_act_task_id__uk",
				  columnNames = { "scenario_act_id", "from_scenario_act_task" }),
		  @UniqueConstraint( name = "scenario_act_task_edge__scenario_act_id__to_scenario_act_task_id__uk",
				  columnNames = { "scenario_act_id", "to_scenario_act_task" }) })
public class ScenarioActTaskEdge {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_task_edge__scenario_act__fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioAct scenarioAct;

	private String fromScenarioActTask;

	private String toScenarioActTask;

	public ScenarioActTaskEdge() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ScenarioAct getScenarioAct() {
		return scenarioAct;
	}

	public void setScenarioActTemplate(ScenarioAct scenarioAct) {
		this.scenarioAct = scenarioAct;
	}


	
	public String getFromScenarioActTask() {
		return fromScenarioActTask;
	}

	public void setFromScenarioActTask(String fromScenarioActTask) {
		this.fromScenarioActTask = fromScenarioActTask;
	}

	public String getToScenarioActTask() {
		return toScenarioActTask;
	}

	public void setToScenarioActTask(String toScenarioActTask) {
		this.toScenarioActTask = toScenarioActTask;
	}

	public void setScenarioAct(ScenarioAct scenarioAct) {
		this.scenarioAct = scenarioAct;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScenarioActTaskEdge that = (ScenarioActTaskEdge) o;
		return Objects.equals(getId(), that.getId())
				&& Objects.equals(getScenarioAct(), that.getScenarioAct())
				&& Objects.equals(getFromScenarioActTask(), that.getFromScenarioActTask())
				&& Objects.equals(getToScenarioActTask(), that.getToScenarioActTask());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getScenarioAct(), getFromScenarioActTask(), getToScenarioActTask());
	}
}
