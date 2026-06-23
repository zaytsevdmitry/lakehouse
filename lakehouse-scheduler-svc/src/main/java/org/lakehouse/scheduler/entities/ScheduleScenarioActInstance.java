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
