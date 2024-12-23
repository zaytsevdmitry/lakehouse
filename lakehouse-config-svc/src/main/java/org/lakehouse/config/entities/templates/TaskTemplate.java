package org.lakehouse.config.entities.templates;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.TaskAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "task_template_scenario_act_template_name_name_uk", columnNames = {
		"scenario_act_template_name", "name" }))
public class TaskTemplate extends TaskAbstract {


	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "task_template__scenario_act_template_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioActTemplate scenarioActTemplate;


	
	public TaskTemplate() {
	}


	public ScenarioActTemplate getScenarioTemplate() {
		return scenarioActTemplate;
	}

	public void setScenarioTemplate(ScenarioActTemplate scenarioActTemplate) {
		this.scenarioActTemplate = scenarioActTemplate;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		TaskTemplate that = (TaskTemplate) o;
		return  Objects.equals(getScenarioTemplate(), that.getScenarioTemplate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getScenarioTemplate());
	}
}
