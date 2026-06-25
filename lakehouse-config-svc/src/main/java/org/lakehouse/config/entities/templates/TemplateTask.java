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

package org.lakehouse.config.entities.templates;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.TaskAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(
        name = "task_template_scenario_act_template_name_name_uk",
        columnNames = {"scenario_act_template_name", "name"}))
public class TemplateTask extends TaskAbstract {


    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template__scenario_act_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TemplateScenarioAct templateScenarioAct;


    public TemplateTask() {
    }


    public TemplateScenarioAct getScenarioTemplate() {
        return templateScenarioAct;
    }

    public void setScenarioTemplate(TemplateScenarioAct templateScenarioAct) {
        this.templateScenarioAct = templateScenarioAct;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        TemplateTask that = (TemplateTask) o;
        return Objects.equals(getScenarioTemplate(), that.getScenarioTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioTemplate());
    }
}
