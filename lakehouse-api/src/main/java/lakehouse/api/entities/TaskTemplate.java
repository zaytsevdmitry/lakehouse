package lakehouse.api.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;


@Entity
public class TaskTemplate extends KeyEntityAbstract {


    private String executionModule;

    private String importance;
    @ManyToOne(cascade = CascadeType.REMOVE)
    private ScenarioTemplate scenarioTemplate;
    @ManyToOne(cascade = CascadeType.DETACH)
    private TaskExecutionServiceGroup taskExecutionServiceGroup;

    public TaskTemplate(){}

    public String getExecutionModule() {
        return executionModule;
    }

    public void setExecutionModule(String executionModule) {
        this.executionModule = executionModule;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
        return taskExecutionServiceGroup;
    }

    public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
        this.taskExecutionServiceGroup = taskExecutionServiceGroup;
    }

    public ScenarioTemplate getScenarioTemplate() {
        return scenarioTemplate;
    }

    public void setScenarioTemplate(ScenarioTemplate scenarioTemplate) {
        this.scenarioTemplate = scenarioTemplate;
    }
}
