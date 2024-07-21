package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;


@Entity
public class TaskTemplateProperty extends KeyValueAbstract {

    @ManyToOne
 //   @JoinColumn(name = "task_template_key", referencedColumnName = "key")
    private TaskTemplate taskTemplate;

    public TaskTemplateProperty() {
    }

    public TaskTemplate getTaskTemplate() {
        return taskTemplate;
    }

    public void setTaskTemplate(TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
    }
}
