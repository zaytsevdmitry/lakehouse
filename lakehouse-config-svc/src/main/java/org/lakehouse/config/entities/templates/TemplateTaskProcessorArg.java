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
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "task_template_execution_module_arg_task_template_id_key_uk", columnNames = {
        "task_template_id", "key"}))
public class TemplateTaskProcessorArg extends KeyValueAbstract {

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_execution_module_arg__task_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TemplateTask templateTask;

    public TemplateTaskProcessorArg() {
    }

    public TemplateTask getTaskTemplate() {
        return templateTask;
    }

    public void setTaskTemplate(TemplateTask templateTask) {
        this.templateTask = templateTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        TemplateTaskProcessorArg that = (TemplateTaskProcessorArg) o;
        return Objects.equals(getTaskTemplate(), that.getTaskTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskTemplate());
    }
}
