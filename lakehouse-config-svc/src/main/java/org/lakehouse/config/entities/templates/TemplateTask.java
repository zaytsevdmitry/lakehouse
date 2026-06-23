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
