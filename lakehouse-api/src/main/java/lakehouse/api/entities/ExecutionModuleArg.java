package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;


@Entity
public class ExecutionModuleArg extends KeyValueAbstract {

    @ManyToOne
 //   @JoinColumn(name = "task_template_key", referencedColumnName = "key")
    private TaskTemplate taskTemplate;

    public ExecutionModuleArg() {
    }

    public TaskTemplate getTaskTemplate() {
        return taskTemplate;
    }

    public void setTaskTemplate(TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
    }
}
