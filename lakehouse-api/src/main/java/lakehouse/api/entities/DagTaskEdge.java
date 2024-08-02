package lakehouse.api.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class DagTaskEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioTemplate scenarioTemplate;

    @ManyToOne
    private TaskTemplate fromTaskTemplate;

    @ManyToOne
    private TaskTemplate toTaskTemplate;

    public DagTaskEdge() {
    }

    public DagTaskEdge(
            ScenarioTemplate scenarioTemplate,
            TaskTemplate fromTaskTemplate,
            TaskTemplate toTaskTemplate) {
        this.scenarioTemplate = scenarioTemplate;
        this.fromTaskTemplate = fromTaskTemplate;
        this.toTaskTemplate = toTaskTemplate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScenarioTemplate getScenarioTemplate() {
        return scenarioTemplate;
    }

    public void setScenarioTemplate(ScenarioTemplate scenarioTemplate) {
        this.scenarioTemplate = scenarioTemplate;
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
