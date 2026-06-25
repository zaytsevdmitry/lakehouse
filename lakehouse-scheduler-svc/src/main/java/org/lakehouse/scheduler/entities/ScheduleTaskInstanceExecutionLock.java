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
