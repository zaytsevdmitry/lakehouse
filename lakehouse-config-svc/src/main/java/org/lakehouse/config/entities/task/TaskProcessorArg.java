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

package org.lakehouse.config.entities.task;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

/**
 * Additional argument of a {@link Task}. The specific performer determines how to use them.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "task_processor_arg_task_id_key_uk", columnNames = {
        "task_id", "key"}))
public class TaskProcessorArg extends KeyValueAbstract {

    @ManyToOne
    @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "task_processor_arg__task_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    public TaskProcessorArg() {
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        TaskProcessorArg that = (TaskProcessorArg) o;
        return Objects.equals(getTask(), that.getTask());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTask());
    }
}
