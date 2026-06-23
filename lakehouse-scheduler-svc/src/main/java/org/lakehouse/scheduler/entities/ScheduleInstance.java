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
import org.lakehouse.client.api.constant.Status;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_instance_uk_name_targetdt_uk", columnNames = {
        "config_schedule_key_name", "target_execution_date_time"}))
public class ScheduleInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String configScheduleKeyName;

    @Column(nullable = false)
    private OffsetDateTime targetExecutionDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status.Schedule status;

    public ScheduleInstance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigScheduleKeyName() {
        return configScheduleKeyName;
    }

    public void setConfigScheduleKeyName(String configScheduleKeyName) {
        this.configScheduleKeyName = configScheduleKeyName;
    }

    public OffsetDateTime getTargetExecutionDateTime() {
        return targetExecutionDateTime;
    }

    public void setTargetExecutionDateTime(OffsetDateTime targetExecutionDateTime) {
        this.targetExecutionDateTime = targetExecutionDateTime;
    }

    public Status.Schedule getStatus() {
        return status;
    }

    public void setStatus(Status.Schedule status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScheduleInstance that = (ScheduleInstance) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getConfigScheduleKeyName(), that.getConfigScheduleKeyName())
                && Objects.equals(getTargetExecutionDateTime(), that.getTargetExecutionDateTime())
                && Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getConfigScheduleKeyName(), getTargetExecutionDateTime(), getStatus());
    }
}
