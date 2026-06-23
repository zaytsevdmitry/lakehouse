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

package org.lakehouse.config.entities;

import jakarta.persistence.*;

import java.util.Objects;

@MappedSuperclass
public abstract class TaskAbstract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;


    @Column(nullable = false)
    private String taskProcessor;

    private String taskProcessorBody;

    @Column(nullable = false)
    private String importance;


    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task__task_execution_service_group_fk"))
    private TaskExecutionServiceGroup taskExecutionServiceGroup;


    public TaskAbstract() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return getName();
    }


    public String getTaskProcessor() {
        return taskProcessor;
    }

    public void setTaskProcessor(String taskProcessor) {
        this.taskProcessor = taskProcessor;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
        return taskExecutionServiceGroup;
    }

    public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
        this.taskExecutionServiceGroup = taskExecutionServiceGroup;
    }

    public String getTaskProcessorBody() {
        return taskProcessorBody;
    }

    public void setTaskProcessorBody(String taskProcessorBody) {
        this.taskProcessorBody = taskProcessorBody;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskAbstract that = (TaskAbstract) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getTaskProcessor(), that.getTaskProcessor()) && Objects.equals(getTaskProcessorBody(), that.getTaskProcessorBody()) && Objects.equals(getImportance(), that.getImportance()) && Objects.equals(getTaskExecutionServiceGroup(), that.getTaskExecutionServiceGroup());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDescription(), getTaskProcessor(), getTaskProcessorBody(), getImportance(), getTaskExecutionServiceGroup());
    }
}
