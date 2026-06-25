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

package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.templates.TemplateScenarioAct;

import java.util.Objects;

/**
 * Описывает один из графов операций над датасетом.
 * Связывает датасет и расписание
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "scenario_act_data_set_name_name_schedule_name_uk", columnNames = {
        "schedule_name", "name"}))
public class ScenarioAct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_data_set_fk"))
    private DataSet dataSet;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "scenario_act__schedule_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Schedule schedule;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "scenario_act__scenario_act_template_fk"))
    private TemplateScenarioAct templateScenarioAct;

    @Column(nullable = false)
    String intervalStart;

    @Column(nullable = false)
    String intervalEnd;

    public ScenarioAct() {
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

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public TemplateScenarioAct getScenarioActTemplate() {
        return templateScenarioAct;
    }

    public void setScenarioActTemplate(TemplateScenarioAct templateScenarioAct) {
        this.templateScenarioAct = templateScenarioAct;
    }

    public String getIntervalStart() {
        return intervalStart;
    }

    public void setIntervalStart(String intervalStart) {
        this.intervalStart = intervalStart;
    }

    public String getIntervalEnd() {
        return intervalEnd;
    }

    public void setIntervalEnd(String intervalEnd) {
        this.intervalEnd = intervalEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioAct that = (ScenarioAct) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getSchedule(), that.getSchedule()) && Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate()) && Objects.equals(getIntervalStart(), that.getIntervalStart()) && Objects.equals(getIntervalEnd(), that.getIntervalEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDataSet(), getSchedule(), getScenarioActTemplate(), getIntervalStart(), getIntervalEnd());
    }
}
