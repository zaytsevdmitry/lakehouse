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

package org.lakehouse.config.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.entities.script.Script;
import org.lakehouse.config.entities.task.Task;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "sql_template_task_key_uk", columnNames = {"task_id", "key"})
}
)
public class SQLTemplate extends KeyValueAbstract {


    @ManyToOne
    @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "sql_template__task_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__driver_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Driver driver;

    @Column(nullable = false, length = 4000)
    @ManyToOne(targetEntity = Script.class, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__script_fk"))
    private String value;

    public SQLTemplate() {
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SQLTemplate that = (SQLTemplate) o;
        return Objects.equals(getTask(), that.getTask()) && Objects.equals(getDriver(), that.getDriver()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTask(), getDriver(), getValue());
    }
}
