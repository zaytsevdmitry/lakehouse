package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "task_template_scenario_act_template_name_name_uk", columnNames = {
		"scenario_act_template_name", "name" }))
public class TaskTemplate extends KeyEntityAbstract {

	@Column(nullable = false)
	private String executionModule;

	@Column(nullable = false)
	private String importance;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "task_template__scenario_act_template_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScenarioActTemplate scenarioActTemplate;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "task_template__task_execution_service_group_fk"))
	private TaskExecutionServiceGroup taskExecutionServiceGroup;

	public TaskTemplate() {
	}

	public String getExecutionModule() {
		return executionModule;
	}

	public void setExecutionModule(String executionModule) {
		this.executionModule = executionModule;
	}

	public String getImportance() {
		return importance;
	}

	public void setImportance(String importance) {
		this.importance = importance;
	}

	public ScenarioActTemplate getScenarioTemplate() {
		return scenarioActTemplate;
	}

	public void setScenarioTemplate(ScenarioActTemplate scenarioActTemplate) {
		this.scenarioActTemplate = scenarioActTemplate;
	}

	public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
		return taskExecutionServiceGroup;
	}

	public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
		this.taskExecutionServiceGroup = taskExecutionServiceGroup;
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
		return Objects.equals(getExecutionModule(), that.getExecutionModule())
				&& Objects.equals(getImportance(), that.getImportance())
				&& Objects.equals(getScenarioTemplate(), that.getScenarioTemplate())
				&& Objects.equals(getTaskExecutionServiceGroup(), that.getTaskExecutionServiceGroup());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getExecutionModule(), getImportance(), getScenarioTemplate(),
				getTaskExecutionServiceGroup());
	}
}
