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

package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.TaskAbstract;

import java.util.Objects;

/**
 * Используется для указания задач.
 * Расширяет и переопределяет заданный шаблон задач
 */
@Entity
@Table(uniqueConstraints =
@UniqueConstraint(
        name = "scenario_act_id__name_uk",
        columnNames = {
                "scenario_act_id",
                "name"}))
public class ScenarioActTask extends TaskAbstract {


    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_task__scenario_act__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioAct scenarioAct;


    public ScenarioActTask() {
    }


    public ScenarioAct getScenarioAct() {
        return scenarioAct;
    }

    public void setScenarioAct(ScenarioAct scenarioAct) {
        this.scenarioAct = scenarioAct;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        ScenarioActTask that = (ScenarioActTask) o;
        return Objects.equals(getScenarioAct(), that.getScenarioAct());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskProcessor(), getImportance(), getScenarioAct(),
                getTaskExecutionServiceGroup());
    }
}
