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

import java.util.Objects;

@Entity
public class ScheduledTaskForProducerMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "scheduled_task_for_producer_message__sti_id_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleTaskInstance scheduleTaskInstance;

    public ScheduledTaskForProducerMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScheduleTaskInstance getScheduleTaskInstance() {
        return scheduleTaskInstance;
    }

    public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
        this.scheduleTaskInstance = scheduleTaskInstance;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledTaskForProducerMessage message = (ScheduledTaskForProducerMessage) o;
        return Objects.equals(getId(), message.getId()) && Objects.equals(getScheduleTaskInstance(), message.getScheduleTaskInstance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScheduleTaskInstance());
    }
}
