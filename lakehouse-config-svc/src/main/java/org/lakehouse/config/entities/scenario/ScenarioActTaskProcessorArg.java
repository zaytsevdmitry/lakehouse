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

package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(
        name = "scenario_act_task_execution_module_arg__scenario_act_task_id__key__uk",
        columnNames = {"scenario_act_task_id", "key"}))
public class ScenarioActTaskProcessorArg extends KeyValueAbstract {

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_task_execution_module_arg__scenario_act_task_id_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioActTask scenarioActTask;

    public ScenarioActTaskProcessorArg() {
    }

    public ScenarioActTask getScenarioActTask() {
        return scenarioActTask;
    }

    public void setScenarioActTask(ScenarioActTask taskTemplate) {
        this.scenarioActTask = taskTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        ScenarioActTaskProcessorArg that = (ScenarioActTaskProcessorArg) o;
        return Objects.equals(getScenarioActTask(), that.getScenarioActTask());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioActTask());
    }
}
