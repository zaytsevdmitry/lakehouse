package lakehouse.api.dto;

import java.util.List;

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
}
