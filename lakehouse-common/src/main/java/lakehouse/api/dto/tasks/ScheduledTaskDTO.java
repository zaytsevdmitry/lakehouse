package lakehouse.api.dto.tasks;

import java.util.Map;

public class ScheduledTaskDTO {
    private String name;
    private String taskExecutionServiceGroupName;
    private String executionModule;
    private String importance;
    private String description;
    private Map<String,String> executionModuleArgs;

    public ScheduledTaskDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskExecutionServiceGroupName() {
        return taskExecutionServiceGroupName;
    }

    public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupName) {
        this.taskExecutionServiceGroupName = taskExecutionServiceGroupName;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getExecutionModuleArgs() {
        return executionModuleArgs;
    }

    public void setExecutionModuleArgs(Map<String, String> executionModuleArgs) {
        this.executionModuleArgs = executionModuleArgs;
    }
}
