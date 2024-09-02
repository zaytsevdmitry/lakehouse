package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

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
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__scenario_act_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioActTemplate scenarioActTemplate;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__from_task_template_fk"))
    private TaskTemplate fromTaskTemplate;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__to_task_template_fk"))
    private TaskTemplate toTaskTemplate;

    public TaskTemplateEdge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScenarioActTemplate getScenarioActTemplate() {
        return scenarioActTemplate;
    }

    public void setScenarioActTemplate(ScenarioActTemplate scenarioActTemplate) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskTemplateEdge that = (TaskTemplateEdge) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate()) && Objects.equals(getFromTaskTemplate(), that.getFromTaskTemplate()) && Objects.equals(getToTaskTemplate(), that.getToTaskTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScenarioActTemplate(), getFromTaskTemplate(), getToTaskTemplate());
    }
}

