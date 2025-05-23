package org.lakehouse.config.entities.templates;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "task_template_execution_module_arg_task_template_id_key_uk", columnNames = {
		"task_template_id", "key" }))
public class TaskTemplateExecutionModuleArg extends KeyValueAbstract {

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "task_template_execution_module_arg__task_template_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TaskTemplate taskTemplate;

	public TaskTemplateExecutionModuleArg() {
	}

	public TaskTemplate getTaskTemplate() {
		return taskTemplate;
	}

	public void setTaskTemplate(TaskTemplate taskTemplate) {
		this.taskTemplate = taskTemplate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		TaskTemplateExecutionModuleArg that = (TaskTemplateExecutionModuleArg) o;
		return Objects.equals(getTaskTemplate(), that.getTaskTemplate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getTaskTemplate());
	}
}
