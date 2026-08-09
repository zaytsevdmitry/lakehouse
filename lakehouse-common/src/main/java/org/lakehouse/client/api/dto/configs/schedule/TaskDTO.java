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

package org.lakehouse.client.api.dto.configs.schedule;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object (DTO) representing a task, containing its configuration,
 * execution parameters, and related templates.
 */

public class TaskDTO {


    /**
     * Name of the task.
     */
    private String name;

    /**
     * Name of the task template
     * */
    private String template;
    /**
     * Pointer to the performer group taking on the execution role for this task.
     */
    private String taskExecutionServiceGroupName;

    /**
     * Pointer to the name of the module executing the task process.
     */
    private String taskProcessor;

    /**
     * Pointer to the name of the pluggable sub-module on the performer side.
     */
    private String taskProcessorBody;

    /**
     * Importance level of the task.
     */
    private String importance;

    /**
     * Brief description of the task.
     */
    private String description;

    /**
     * Points to the key of a {@link DriverDTO} object.
     */
    private String driverKeyName;

    /**
     * Set of templates implementing generalized actions tailored for the execution system.
     */
    private SQLTemplateDTO sqlTemplate;

    /**
     * Additional task arguments. The specific performer determines how to use them.
     */
    private Map<String, String> taskProcessorArgs = new HashMap<>();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTaskExecutionServiceGroupName() {
        return taskExecutionServiceGroupName;
    }

    public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupName) {
        this.taskExecutionServiceGroupName = taskExecutionServiceGroupName;
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

    public String getDriverKeyName() {
        return driverKeyName;
    }

    public void setDriverKeyName(String driverKeyName) {
        this.driverKeyName = driverKeyName;
    }

    public SQLTemplateDTO getSqlTemplate() {
        return sqlTemplate;
    }

    public void setSqlTemplate(SQLTemplateDTO sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public String getTaskProcessorBody() {
        return taskProcessorBody;
    }

    public void setTaskProcessorBody(String taskProcessorBody) {
        this.taskProcessorBody = taskProcessorBody;
    }

    @Override
    public String toString() {
        return "TaskDTO{" +
                "name='" + name + '\'' +
                ", template='" + template + '\'' +
                ", taskExecutionServiceGroupName='" + taskExecutionServiceGroupName + '\'' +
                ", taskProcessor='" + taskProcessor + '\'' +
                ", taskProcessorBody='" + taskProcessorBody + '\'' +
                ", importance='" + importance + '\'' +
                ", description='" + description + '\'' +
                ", driverKeyName='" + driverKeyName + '\'' +
                ", sqlTemplate=" + sqlTemplate +
                ", taskProcessorArgs=" + taskProcessorArgs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskDTO taskDTO = (TaskDTO) o;
        return Objects.equals(getName(), taskDTO.getName()) && Objects.equals(getTemplate(), taskDTO.getTemplate()) && Objects.equals(getTaskExecutionServiceGroupName(), taskDTO.getTaskExecutionServiceGroupName()) && Objects.equals(getTaskProcessor(), taskDTO.getTaskProcessor()) && Objects.equals(getTaskProcessorBody(), taskDTO.getTaskProcessorBody()) && Objects.equals(getImportance(), taskDTO.getImportance()) && Objects.equals(getDescription(), taskDTO.getDescription()) && Objects.equals(getDriverKeyName(), taskDTO.getDriverKeyName()) && Objects.equals(getSqlTemplate(), taskDTO.getSqlTemplate()) && Objects.equals(getTaskProcessorArgs(), taskDTO.getTaskProcessorArgs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTemplate(), getTaskExecutionServiceGroupName(), getTaskProcessor(), getTaskProcessorBody(), getImportance(), getDescription(), getDriverKeyName(), getSqlTemplate(), getTaskProcessorArgs());
    }

}
