package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class DagTaskEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scenario_key", referencedColumnName = "key")
    private ScenarioTemplate scenarioTemplate;

    @ManyToOne
    @JoinColumn(name = "from_key", referencedColumnName = "key")
    private TaskTemplate fromKey;

    @ManyToOne
    @JoinColumn(name = "to_key", referencedColumnName = "key")
    private TaskTemplate toKey;

    public DagTaskEdge() {
    }

    public DagTaskEdge( ScenarioTemplate scenarioTemplate, TaskTemplate fromKey, TaskTemplate toKey) {
        this.scenarioTemplate = scenarioTemplate;
        this.fromKey = fromKey;
        this.toKey = toKey;
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

    public TaskTemplate getFromKey() {
        return fromKey;
    }

    public void setFromKey(TaskTemplate fromKey) {
        this.fromKey = fromKey;
    }

    public TaskTemplate getToKey() {
        return toKey;
    }

    public void setToKey(TaskTemplate toKey) {
        this.toKey = toKey;
    }
}
