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
