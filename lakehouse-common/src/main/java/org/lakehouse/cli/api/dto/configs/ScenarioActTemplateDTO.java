package org.lakehouse.cli.api.dto.configs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScenarioActTemplateDTO {
    private String name;
    private String description;
    private List<TaskDTO> tasks = new ArrayList<>();
    private List<DagEdgeDTO> dagEdges = new ArrayList<>();
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

    public List<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDTO> tasks) {
        this.tasks = tasks
                .stream()
                .sorted(Comparator.comparing(TaskDTO::hashCode))
                .toList();
    }

    public List<DagEdgeDTO> getDagEdges() {
        return dagEdges;
    }

    public void setDagEdges(List<DagEdgeDTO> dagEdges) {
        this.dagEdges = dagEdges
                .stream() // sort for stable list comparison
                .sorted(Comparator.comparing(DagEdgeDTO::hashCode))
                .toList();
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
