package lakehouse.api.entities.configs;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;


@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(
            name = "task_template_execution_module_arg_task_template_name_name_uk",
            columnNames = {"task_template_name", "name"}))
public class TaskTemplateExecutionModuleArg extends KeyValueAbstract {

    @ManyToOne
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TaskTemplateExecutionModuleArg that = (TaskTemplateExecutionModuleArg) o;
        return Objects.equals(getTaskTemplate(), that.getTaskTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskTemplate());
    }
}
