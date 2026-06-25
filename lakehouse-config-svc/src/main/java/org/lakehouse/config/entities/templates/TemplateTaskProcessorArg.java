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
