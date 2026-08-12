/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.config.service.dataset;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataSetLineageService {

    private final DataSetSourceRepository dataSetSourceRepository;

    public DataSetLineageService(DataSetSourceRepository dataSetSourceRepository) {
        this.dataSetSourceRepository = dataSetSourceRepository;
    }

    public DataSetLineageDTO findLineage(String dataSetKeyName) {
        List<String> vertices = new ArrayList<>();
        List<DagEdgeDTO> edges = new ArrayList<>();
        if (dataSetKeyName == null) {
            return buildResult(vertices, edges);
        }

        Set<String> visitedVertices = new HashSet<>();
        Set<String> visitedEdges = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        addVertex(vertices, visitedVertices, dataSetKeyName);
        queue.add(dataSetKeyName);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            dataSetSourceRepository.findBySourceKeyName(current)
                    .stream()
                    .filter(dataSetSource -> dataSetSource.getDataSet() != null)
                    .forEach(dataSetSource -> {
                        String target = dataSetSource.getDataSet().getKeyName();
                        addEdge(edges, visitedEdges, current, target);
                        if (addVertex(vertices, visitedVertices, target)) {
                            queue.add(target);
                        }
                    });

            dataSetSourceRepository.findByDataSetKeyName(current)
                    .stream()
                    .filter(dataSetSource -> dataSetSource.getSource() != null)
                    .forEach(dataSetSource -> {
                        String source = dataSetSource.getSource().getKeyName();
                        addEdge(edges, visitedEdges, source, current);
                        if (addVertex(vertices, visitedVertices, source)) {
                            queue.add(source);
                        }
                    });
        }

        return buildResult(vertices, edges);
    }

    private boolean addVertex(List<String> vertices, Set<String> visited, String vertex) {
        if (vertex != null && visited.add(vertex)) {
            vertices.add(vertex);
            return true;
        }
        return false;
    }

    private void addEdge(List<DagEdgeDTO> edges, Set<String> visitedEdges, String from, String to) {
        if (from == null || to == null || !visitedEdges.add(from + ">" + to)) {
            return;
        }
        DagEdgeDTO edge = new DagEdgeDTO();
        edge.setFrom(from);
        edge.setTo(to);
        edges.add(edge);
    }

    private DataSetLineageDTO buildResult(List<String> vertices, List<DagEdgeDTO> edges) {
        DataSetLineageDTO result = new DataSetLineageDTO();
        result.setVertices(vertices);
        result.setEdges(edges);
        return result;
    }
}
