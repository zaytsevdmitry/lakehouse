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

package org.lakehouse.client.api.dto.scheduler;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScheduleInstanceDAGDTO extends ScheduleInstanceDTO {
    private List<ScheduleScenarioActInstanceDTO> scenarioActs = new ArrayList<>();

    private List<DagEdgeDTO> scenarioActEdges = new ArrayList<>();

    public ScheduleInstanceDAGDTO() {
    }

    public List<ScheduleScenarioActInstanceDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(List<ScheduleScenarioActInstanceDTO> scenarioActs) {
        this.scenarioActs = scenarioActs;
    }

    public List<DagEdgeDTO> getScenarioActEdges() {
        return scenarioActEdges;
    }

    public void setScenarioActEdges(List<DagEdgeDTO> scenarioActEdges) {
        this.scenarioActEdges = scenarioActEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        ScheduleInstanceDAGDTO that = (ScheduleInstanceDAGDTO) o;
        return Objects.equals(getScenarioActs(), that.getScenarioActs())
                && Objects.equals(getScenarioActEdges(), that.getScenarioActEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioActs(), getScenarioActEdges());
    }

    @Override
    public String toString() {
        return "ScheduleInstanceDAGDTO{" +
                "id=" + getId() +
                ", configScheduleKeyName='" + getConfigScheduleKeyName() + '\'' +
                ", targetExecutionDateTime='" + getTargetExecutionDateTime() + '\'' +
                ", status=" + getStatus() +
                ", scenarioActs=" + scenarioActs +
                ", scenarioActEdges=" + scenarioActEdges +
                '}';
    }
}
