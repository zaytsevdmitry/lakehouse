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

import java.time.OffsetDateTime;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "schedule_task_instance_execution_lock_id_uk", columnNames = {"id"}),
        @UniqueConstraint(name = "schedule_task_instance_execution_lock_schedule_task_instance_id_uk", columnNames = {"schedule_task_instance_id"}),
}
)
public class ScheduleTaskInstanceExecutionLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private OffsetDateTime lastHeartBeatDateTime;

    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "schedule_task_instance_execution_lock__sti_id_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleTaskInstance scheduleTaskInstance;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public ScheduleTaskInstance getScheduleTaskInstance() {
        return scheduleTaskInstance;
    }

    public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
        this.scheduleTaskInstance = scheduleTaskInstance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getLastHeartBeatDateTime() {
        return lastHeartBeatDateTime;
    }

    public void setLastHeartBeatDateTime(OffsetDateTime lastHeartBeatDateTime) {
        this.lastHeartBeatDateTime = lastHeartBeatDateTime;
    }

    @Override
    public String toString() {
        return "ScheduleTaskInstanceExecutionLock{" +
                "id=" + id +
                ", serviceId='" + serviceId + '\'' +
                ", lastHeartBeatDateTime=" + lastHeartBeatDateTime +
                ", scheduleTaskInstance=" + scheduleTaskInstance +
                '}';
    }
}
