package lakehouse.api.dto.configs;

import java.util.ArrayList;
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
        ScenarioActTemplateDTO that = (ScenarioActTemplateDTO) o;
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
