package org.lakehouse.client.api.dto.configs;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScenarioActTemplateDTO {
    private String name;
    private String description;
    private Set<TaskDTO> tasks = new HashSet<>();
    private Set<DagEdgeDTO> dagEdges = new HashSet<>();

    public ScenarioActTemplateDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(Set<TaskDTO> tasks) {
        this.tasks = tasks;
    }

    public Set<DagEdgeDTO> getDagEdges() {
        return dagEdges;
    }

    public void setDagEdges(Set<DagEdgeDTO> dagEdges) {
        this.dagEdges = dagEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioActTemplateDTO that = (ScenarioActTemplateDTO) o;

        return Objects.equals(getName(), that.getName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(this.getTasks(), that.getTasks())
                && Objects.equals(getDagEdges(), that.getDagEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), getTasks(), getDagEdges());
    }
}
