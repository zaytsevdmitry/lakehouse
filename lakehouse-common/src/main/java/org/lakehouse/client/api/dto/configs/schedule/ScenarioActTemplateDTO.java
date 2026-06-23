/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
