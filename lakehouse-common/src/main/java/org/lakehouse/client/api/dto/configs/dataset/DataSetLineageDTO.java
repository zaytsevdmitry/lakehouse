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
