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

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_task_instance_schedule_scenario_act_instance_id_name_uk", columnNames = {
        "schedule_scenario_act_instance_id", "name"}))
public class ScheduleTaskInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "schedule_task_instance__ssai_id_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleScenarioActInstance scheduleScenarioActInstance;

    @Column()
    private OffsetDateTime beginDateTime;

    @Column()
    private OffsetDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status.Task status;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int reTryNum = 0;

    private String serviceId;

    private String causes;

    public ScheduleTaskInstance() {
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

    public OffsetDateTime getBeginDateTime() {
        return beginDateTime;
    }

    public void setBeginDateTime(OffsetDateTime beginDateTime) {
        this.beginDateTime = beginDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Status.Task getStatus() {
        return status;
    }

    public void setStatus(Status.Task status) {
        this.status = status;
    }

    public int getReTryNum() {
        return reTryNum;
    }

    public void setReTryNum(int reTryNum) {
        this.reTryNum = reTryNum;
    }

    public ScheduleScenarioActInstance getScheduleScenarioActInstance() {
        return scheduleScenarioActInstance;
    }

    public void setScheduleScenarioActInstance(ScheduleScenarioActInstance scheduleScenarioActInstance) {
        this.scheduleScenarioActInstance = scheduleScenarioActInstance;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        if (causes != null)
            this.causes = causes.substring(0, Math.min(causes.length(), 254));
        else
            this.causes = causes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleTaskInstance that = (ScheduleTaskInstance) o;
        return getReTryNum() == that.getReTryNum() && Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getScheduleScenarioActInstance(), that.getScheduleScenarioActInstance()) && Objects.equals(getBeginDateTime(), that.getBeginDateTime()) && Objects.equals(getEndDateTime(), that.getEndDateTime()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getServiceId(), that.getServiceId()) && Objects.equals(getCauses(), that.getCauses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getScheduleScenarioActInstance(), getBeginDateTime(), getEndDateTime(), getStatus(), getReTryNum(), getServiceId(), getCauses());
    }

    @Override
    public String toString() {
        return "ScheduleTaskInstance{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", scheduleScenarioActInstance=" + scheduleScenarioActInstance +
                ", beginDateTime=" + beginDateTime +
                ", endDateTime=" + endDateTime +
                ", status='" + status + '\'' +
                ", reTryCount=" + reTryNum +
                ", serviceId='" + serviceId + '\'' +
                ", causes='" + causes + '\'' +
                '}';
    }
}
