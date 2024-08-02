package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.Objects;


@Entity
public class ExecutionModuleArg extends KeyValueAbstract {

    @ManyToOne
    private TaskTemplate taskTemplate;

    public ExecutionModuleArg() {
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
        ExecutionModuleArg that = (ExecutionModuleArg) o;
        return Objects.equals(getTaskTemplate(), that.getTaskTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskTemplate());
    }
}
