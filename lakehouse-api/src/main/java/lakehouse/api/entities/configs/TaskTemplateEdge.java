package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(uniqueConstraints ={
    @UniqueConstraint(
            name = "task_template_edge_data_set_name_name_uk",
            columnNames = {"data_set_name", "name"}),
    @UniqueConstraint(name = "task_template_edge_from_to_uk",
            columnNames = {"scenario_act_template_name","from_task_template_name","to_task_template_name"})
})
public class TaskTemplateEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioActTemplate scenarioActTemplate;

    @ManyToOne
    private TaskTemplate fromTaskTemplate;

    @ManyToOne
    private TaskTemplate toTaskTemplate;

    public TaskTemplateEdge() {
    }

    public TaskTemplateEdge(
            ScenarioActTemplate scenarioActTemplate,
            TaskTemplate fromTaskTemplate,
            TaskTemplate toTaskTemplate) {
        this.scenarioActTemplate = scenarioActTemplate;
        this.fromTaskTemplate = fromTaskTemplate;
        this.toTaskTemplate = toTaskTemplate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScenarioActTemplate getScenarioTemplate() {
        return scenarioActTemplate;
    }

    public void setScenarioTemplate(ScenarioActTemplate scenarioActTemplate) {
        this.scenarioActTemplate = scenarioActTemplate;
    }

    public TaskTemplate getFromTaskTemplate() {
        return fromTaskTemplate;
    }

    public void setFromTaskTemplate(TaskTemplate fromTaskTemplate) {
        this.fromTaskTemplate = fromTaskTemplate;
    }

    public TaskTemplate getToTaskTemplate() {
        return toTaskTemplate;
    }

    public void setToTaskTemplate(TaskTemplate toTaskTemplate) {
        this.toTaskTemplate = toTaskTemplate;
    }
}
