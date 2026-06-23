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
