package org.lakehouse.cli.api.dto.configs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskDTO {
    private String name;
    private String taskExecutionServiceGroupName;
    private String executionModule;
    private String importance;
    private String description;
    private Map<String,String> executionModuleArgs = new HashMap<>();
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskExecutionServiceGroupName() {
        return taskExecutionServiceGroupName;
    }

    public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupkey) {
        this.taskExecutionServiceGroupName = taskExecutionServiceGroupkey;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDTO taskDTO = (TaskDTO) o;
        return Objects.equals(name, taskDTO.name)
                && Objects.equals(taskExecutionServiceGroupName, taskDTO.taskExecutionServiceGroupName)
                && Objects.equals(executionModule, taskDTO.executionModule)
                && Objects.equals(importance, taskDTO.importance)
                && Objects.equals(description, taskDTO.description)
                && Objects.equals(executionModuleArgs, taskDTO.executionModuleArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, taskExecutionServiceGroupName, executionModule, importance, description, executionModuleArgs);
    }
}
