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
import org.lakehouse.config.entities.TaskExecutionServiceGroup;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.templates.TemplateScenarioAct;

/**
 * Unified task entity used for all three {@link org.lakehouse.client.api.dto.configs.schedule.TaskDTO} usage cases:
 * <ul>
 *     <li>as a task of a {@link TemplateScenarioAct} (when {@link #templateScenarioAct} is set);</li>
 *     <li>as a task of a {@link ScenarioAct} (when {@link #scenarioAct} is set);</li>
 *     <li>as a standalone task (when both parent references are {@code null}).</li>
 * </ul>
 * A task may optionally reference another task acting as its template via {@link #template}.
 */
@Entity

@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "task_template_scenario_act_template_name_name_uk", columnNames = {"scenario_act_template_name", "name"}),
                @UniqueConstraint(name = "task_scenario_act_id_name_uk", columnNames = {"scenario_act_id", "name"})
                },
        indexes = {

                @Index(
                        name = "task_standalone_name_uidx",
                        columnList = "name, scenario_act_template_name, scenario_act_id",
                        unique = true
                )
        }
        )
public class Task  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String description;


    @Column(nullable = true)
    private String taskProcessor;

    private String taskProcessorBody;

    @Column(nullable = true)
    private String importance;

    @Column(nullable = true)
    private Integer maxRetries;


    @ManyToOne
    @JoinColumn(name = "driver_id", foreignKey = @ForeignKey(name = "task__driver_fk"))
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "task_execution_service_group_key_name", foreignKey = @ForeignKey(name = "task__task_execution_service_group_fk"))
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private TaskExecutionServiceGroup taskExecutionServiceGroup;


    @ManyToOne
    @JoinColumn(name = "scenario_act_template_name", foreignKey = @ForeignKey(name = "task__scenario_act_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TemplateScenarioAct templateScenarioAct;

    @ManyToOne
    @JoinColumn(name = "scenario_act_id", foreignKey = @ForeignKey(name = "task__scenario_act_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioAct scenarioAct;

    /**
     * Name of the task acting as a template for this task.
     */
    @Column(name = "template_name")
    private String template;

    public Task() {
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

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
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

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public void setTemplateScenarioAct(TemplateScenarioAct templateScenarioAct) {
        if (templateScenarioAct != null && this.scenarioAct != null) {
            throw new IllegalArgumentException("Cannot set TemplateScenarioAct when ScenarioAct is already assigned.");
        }
        this.templateScenarioAct = templateScenarioAct;
    }

    public void setScenarioAct(ScenarioAct scenarioAct) {
        if (scenarioAct != null && this.templateScenarioAct != null) {
            throw new IllegalArgumentException("Cannot set ScenarioAct when TemplateScenarioAct is already assigned.");
        }
        this.scenarioAct = scenarioAct;
    }

    public TemplateScenarioAct getTemplateScenarioAct() {
        return templateScenarioAct;
    }

    public ScenarioAct getScenarioAct() {
        return scenarioAct;
    }


    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }


}
