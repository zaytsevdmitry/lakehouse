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
import org.lakehouse.config.entities.task.Task;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "task_template_edge_from_to_uk", columnNames = {"scenario_act_template_name",
                "from_task_template_id", "to_task_template_id"})})
public class TemplateTaskEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__scenario_act_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TemplateScenarioAct templateScenarioAct;

    @ManyToOne
    @JoinColumn(name = "from_task_id", foreignKey = @ForeignKey(name = "task_template_edge__from_task_fk"))
    private Task fromTask;

    @ManyToOne
    @JoinColumn(name = "to_task_id", foreignKey = @ForeignKey(name = "task_template_edge__to_task_fk"))
    private Task toTask;

    public TemplateTaskEdge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TemplateScenarioAct getScenarioActTemplate() {
        return templateScenarioAct;
    }

    public void setScenarioActTemplate(TemplateScenarioAct templateScenarioAct) {
        this.templateScenarioAct = templateScenarioAct;
    }

    public Task getFromTask() {
        return fromTask;
    }

    public void setFromTask(Task fromTask) {
        this.fromTask = fromTask;
    }

    public Task getToTask() {
        return toTask;
    }

    public void setToTask(Task toTask) {
        this.toTask = toTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TemplateTaskEdge that = (TemplateTaskEdge) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate())
                && Objects.equals(getFromTask(), that.getFromTask())
                && Objects.equals(getToTask(), that.getToTask());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScenarioActTemplate(), getFromTask(), getToTask());
    }
}
