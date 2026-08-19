package org.lakehouse.client.api.dto.configs.dataset;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;

import java.util.ArrayList;
import java.util.List;


public class DataSetLineageDTO {
    List<String> vertices = new ArrayList<>();
    List<DagEdgeDTO> edges = new ArrayList<>();

    public DataSetLineageDTO() {
    }

    public List<String> getVertices() {
        return vertices;
    }

    public void setVertices(List<String> vertices) {
        this.vertices = vertices;
    }

    public List<DagEdgeDTO> getEdges() {
        return edges;
    }

    public void setEdges(List<DagEdgeDTO> edges) {
        this.edges = edges;
    }
}
