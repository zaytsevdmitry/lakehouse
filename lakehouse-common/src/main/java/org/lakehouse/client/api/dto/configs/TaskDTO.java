package org.lakehouse.client.api.dto.configs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskDTO {
    private String name;
    private String taskExecutionServiceGroupName;
    private String taskProcessor;
    private String taskProcessorBody;
    private String importance;
    private String description;
    private Map<String, String> taskProcessorArgs = new HashMap<>();

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

    public String getTaskProcessor() {
        return taskProcessor;
    }

    public void setTaskProcessor(String taskProcessor) {
        this.taskProcessor = taskProcessor;
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

    public Map<String, String> getTaskProcessorArgs() {
        return taskProcessorArgs;
    }

    public void setTaskProcessorArgs(Map<String, String> taskProcessorArgs) {
        this.taskProcessorArgs = taskProcessorArgs;
    }

    public String getTaskProcessorBody() {
        return taskProcessorBody;
    }

    public void setTaskProcessorBody(String taskProcessorBody) {
        this.taskProcessorBody = taskProcessorBody;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskDTO taskDTO = (TaskDTO) o;
        return Objects.equals(getName(), taskDTO.getName()) && Objects.equals(getTaskExecutionServiceGroupName(), taskDTO.getTaskExecutionServiceGroupName()) && Objects.equals(getTaskProcessor(), taskDTO.getTaskProcessor()) && Objects.equals(getTaskProcessorBody(), taskDTO.getTaskProcessorBody()) && Objects.equals(getImportance(), taskDTO.getImportance()) && Objects.equals(getDescription(), taskDTO.getDescription()) && Objects.equals(getTaskProcessorArgs(), taskDTO.getTaskProcessorArgs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTaskExecutionServiceGroupName(), getTaskProcessor(), getTaskProcessorBody(), getImportance(), getDescription(), getTaskProcessorArgs());
    }

    @Override
    public String toString() {
        return "TaskDTO{" + "name='" + name + '\'' + ", taskExecutionServiceGroupName='" + taskExecutionServiceGroupName + '\'' + ", taskProcessor='" + taskProcessor + '\'' + ", taskProcessorBody='" + taskProcessorBody + '\'' + ", importance='" + importance + '\'' + ", description='" + description + '\'' + ", taskProcessorArgs=" + taskProcessorArgs + '}';
    }
}
