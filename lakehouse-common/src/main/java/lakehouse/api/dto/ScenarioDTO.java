package lakehouse.api.dto;

import java.util.List;
import java.util.Objects;

public class ScenarioDTO {
    private String name;
    private String description;
    private List<TaskDTO> tasks;
    private List<DagEdgeDTO> dagEdges;
    public ScenarioDTO() {
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
        this.tasks = tasks;
    }

    public List<DagEdgeDTO> getDagEdges() {
        return dagEdges;
    }

    public void setDagEdges(List<DagEdgeDTO> dagEdges) {
        this.dagEdges = dagEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioDTO that = (ScenarioDTO) o;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(tasks, that.tasks)
                && Objects.equals(dagEdges, that.dagEdges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, tasks, dagEdges);
    }
}
