package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


@Entity
public class TaskTemplate extends KeyEntityAbstract {


    private String executionModule;

    private String importance;
    @ManyToOne
    @JoinColumn(name = "scenario_key", referencedColumnName = "key")
    private ScenarioTemplate scenarioTemplate;
    @ManyToOne
    @JoinColumn(name = "task_execution_service_group_key", referencedColumnName = "key")
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
}
