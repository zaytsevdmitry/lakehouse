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

package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Status;

import java.util.Objects;

@Entity
public class ScheduleScenarioActInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false)
    private ScheduleInstance scheduleInstance;

    @Column(nullable = false)
    private String confDataSetKeyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status.ScenarioAct status;

    public ScheduleScenarioActInstance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScheduleInstance getScheduleInstance() {
        return scheduleInstance;
    }

    public void setScheduleInstance(ScheduleInstance scheduleInstance) {
        this.scheduleInstance = scheduleInstance;
    }

    public String getConfDataSetKeyName() {
        return confDataSetKeyName;
    }

    public void setConfDataSetKeyName(String confDataSetKeyName) {
        this.confDataSetKeyName = confDataSetKeyName;
    }

    public Status.ScenarioAct getStatus() {
        return status;
    }

    public void setStatus(Status.ScenarioAct status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScheduleScenarioActInstance that = (ScheduleScenarioActInstance) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName())
                && Objects.equals(getScheduleInstance(), that.getScheduleInstance())
                && Objects.equals(getConfDataSetKeyName(), that.getConfDataSetKeyName()) && Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getScheduleInstance(), getConfDataSetKeyName(), getStatus());
    }

    @Override
    public String toString() {
        return "ScheduleScenarioActInstance{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", scheduleInstance=" + scheduleInstance +
                ", confDataSetKeyName='" + confDataSetKeyName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
