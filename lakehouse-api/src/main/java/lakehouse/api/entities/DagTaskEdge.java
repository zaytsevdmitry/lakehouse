package lakehouse.api.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class DagTaskEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    private ScenarioTemplate scenarioTemplate;

    @ManyToOne(cascade = CascadeType.REMOVE)
    private TaskTemplate fromTaskTemplate;

    @ManyToOne(cascade = CascadeType.REMOVE)
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
