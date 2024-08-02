package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;


@Entity
public class TaskTemplate extends KeyEntityAbstract {


    private String executionModule;

    private String importance;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioTemplate scenarioTemplate;
    @ManyToOne
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TaskTemplate that = (TaskTemplate) o;
        return Objects.equals(getExecutionModule(), that.getExecutionModule()) && Objects.equals(getImportance(), that.getImportance()) && Objects.equals(getScenarioTemplate(), that.getScenarioTemplate()) && Objects.equals(getTaskExecutionServiceGroup(), that.getTaskExecutionServiceGroup());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getExecutionModule(), getImportance(), getScenarioTemplate(), getTaskExecutionServiceGroup());
    }
}
