package org.lakehouse.cli.api.dto.configs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleScenarioActAbstract {
    private String name;
    private String dataSet;
    private List<TaskDTO> tasks = new ArrayList<>();
    private List<DagEdgeDTO> dagEdges = new ArrayList<>();

    public ScheduleScenarioActAbstract() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
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
        ScheduleScenarioActAbstract that = (ScheduleScenarioActAbstract) o;
        return Objects.equals(getName(), that.getName()) 
        		&& Objects.equals(getDataSet(), that.getDataSet()) 

        		&& Objects.equals(this.getTasks(), that.getTasks())
                && Objects.equals(getDagEdges(), that.getDagEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDataSet());
    }
}
