package org.lakehouse.client.api.dto.configs.schedule;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.KeyNameDescriptionAbstract;

import java.io.Serial;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScenarioActTemplateDTO extends KeyNameDescriptionAbstract {
    @Serial
    private static final long serialVersionUID = 2789430311171934926L;
    private Set<TaskDTO> tasks = new HashSet<>();
    private Set<DagEdgeDTO> dagEdges = new HashSet<>();

    public ScenarioActTemplateDTO() {
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

        return Objects.equals(getKeyName(), that.getKeyName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(this.getTasks(), that.getTasks())
                && Objects.equals(getDagEdges(), that.getDagEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getDescription(), getTasks(), getDagEdges());
    }
}
