package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.TaskAbstract;

import java.util.Objects;
/**
 * Используется для указания задач.
 * Расширяет и переопределяет заданный шаблон задач
 * */
@Entity
@Table(uniqueConstraints = 
	@UniqueConstraint(
			name = "scenario_act_id__name_uk",
			columnNames = {
					"scenario_act_id",
					"name" }))
public class ScenarioActTask extends TaskAbstract {

	
	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_task__scenario_act__fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioAct scenarioAct;

	
	
	
	public ScenarioActTask() {
	}

	
	
	public ScenarioAct getScenarioAct() {
		return scenarioAct;
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
		if (!super.equals(o))
			return false;
		ScenarioActTask that = (ScenarioActTask) o;
		return Objects.equals(getScenarioAct(), that.getScenarioAct());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getExecutionModule(), getImportance(), getScenarioAct(),
				getTaskExecutionServiceGroup());
	}
}
