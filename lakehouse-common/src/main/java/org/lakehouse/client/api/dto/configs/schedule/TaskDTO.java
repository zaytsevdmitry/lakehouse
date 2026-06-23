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

package org.lakehouse.client.api.dto.configs.schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskDTO {
    private String name;
    private String taskExecutionServiceGroupName;
    private String taskProcessor;
    private String taskProcessorBody;
    private String importance;
    private String description;
    private Map<String, String> taskProcessorArgs = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskExecutionServiceGroupName() {
        return taskExecutionServiceGroupName;
    }

    public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupkey) {
        this.taskExecutionServiceGroupName = taskExecutionServiceGroupkey;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getTaskProcessorArgs() {
        return taskProcessorArgs;
    }

    public void setTaskProcessorArgs(Map<String, String> taskProcessorArgs) {
        this.taskProcessorArgs = taskProcessorArgs;
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
        TaskDTO taskDTO = (TaskDTO) o;
        return Objects.equals(getName(), taskDTO.getName()) && Objects.equals(getTaskExecutionServiceGroupName(), taskDTO.getTaskExecutionServiceGroupName()) && Objects.equals(getTaskProcessor(), taskDTO.getTaskProcessor()) && Objects.equals(getTaskProcessorBody(), taskDTO.getTaskProcessorBody()) && Objects.equals(getImportance(), taskDTO.getImportance()) && Objects.equals(getDescription(), taskDTO.getDescription()) && Objects.equals(getTaskProcessorArgs(), taskDTO.getTaskProcessorArgs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTaskExecutionServiceGroupName(), getTaskProcessor(), getTaskProcessorBody(), getImportance(), getDescription(), getTaskProcessorArgs());
    }

    @Override
    public String toString() {
        return "TaskDTO{" + "name='" + name + '\'' + ", taskExecutionServiceGroupName='" + taskExecutionServiceGroupName + '\'' + ", taskProcessor='" + taskProcessor + '\'' + ", taskProcessorBody='" + taskProcessorBody + '\'' + ", importance='" + importance + '\'' + ", description='" + description + '\'' + ", taskProcessorArgs=" + taskProcessorArgs + '}';
    }
}
