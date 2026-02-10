package org.lakehouse.client.api.dto.configs.schedule;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;

import java.util.*;
import java.util.stream.Collectors;

public class ScheduleScenarioActAbstract {
    private String name;
    private String dataSet;
    private Set<TaskDTO> tasks = new HashSet<>();
    private Set<DagEdgeDTO> dagEdges = new HashSet<>();
    private String intervalStart;
    private String intervalEnd;

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

    public Set<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(Set<TaskDTO> tasks) {
        this.tasks = tasks.stream().sorted(Comparator.comparing(TaskDTO::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<DagEdgeDTO> getDagEdges() {
        return dagEdges;
    }

    public void setDagEdges(Set<DagEdgeDTO> dagEdges) {
        this.dagEdges = dagEdges.stream().sorted(Comparator.comparing(DagEdgeDTO::getFrom).thenComparing(DagEdgeDTO::getTo)).collect(Collectors.toCollection(LinkedHashSet::new));;
    }

    public String getIntervalStart() {
        return intervalStart;
    }

    public void setIntervalStart(String intervalStart) {
        this.intervalStart = intervalStart;
    }

    public String getIntervalEnd() {
        return intervalEnd;
    }

    public void setIntervalEnd(String intervalEnd) {
        this.intervalEnd = intervalEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleScenarioActAbstract that = (ScheduleScenarioActAbstract) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getDataSet(), that.getDataSet())
                && Objects.equals(getTasks(), that.getTasks())
                && Objects.equals(getDagEdges(), that.getDagEdges())
                && Objects.equals(getIntervalStart(), that.getIntervalStart())
                && Objects.equals(getIntervalEnd(), that.getIntervalEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDataSet(), getTasks(), getDagEdges(), getIntervalStart(), getIntervalEnd());
    }
}
