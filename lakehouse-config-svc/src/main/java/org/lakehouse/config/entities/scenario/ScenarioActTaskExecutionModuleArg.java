package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints =
	@UniqueConstraint(
			name = "scenario_act_task_execution_module_arg__scenario_act_task_id__name__uk", 
			columnNames = {"scenario_act_task_id", "name" }))
public class ScenarioActTaskExecutionModuleArg extends KeyValueAbstract {

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_task_execution_module_arg__scenario_act_task_id_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioActTask scenarioActTask;

	public ScenarioActTaskExecutionModuleArg() {
	}

	public ScenarioActTask getScenarioActTask() {
		return scenarioActTask;
	}

	public void setScenarioActTask(ScenarioActTask taskTemplate) {
		this.scenarioActTask = taskTemplate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		ScenarioActTaskExecutionModuleArg that = (ScenarioActTaskExecutionModuleArg) o;
		return Objects.equals(getScenarioActTask(), that.getScenarioActTask());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getScenarioActTask());
	}
}
