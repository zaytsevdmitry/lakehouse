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
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "schedule_instance_last_build__schedule_name_uk", columnNames = {"config_schedule_key_name"}),
        @UniqueConstraint(name = "schedule_instance_last_build__si_id_uk", columnNames = {"schedule_instance_id"})
}
)
public class ScheduleInstanceLastBuild {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String configScheduleKeyName;

    @OneToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ScheduleInstance scheduleInstance;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @Column(nullable = false)
    private Long lastChangeNumber;

    @Column(nullable = false)
    private OffsetDateTime lastChangedDateTime;

    public ScheduleInstanceLastBuild() {
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

    public ScheduleInstance getScheduleInstance() {
        return scheduleInstance;
    }

    public void setScheduleInstance(ScheduleInstance scheduleInstance) {
        this.scheduleInstance = scheduleInstance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public void setLastUpdateDateTime(OffsetDateTime lastUpdateDateTime) {
        this.lastUpdateDateTime = lastUpdateDateTime;
    }

    public void setLastChangeNumber(Long lastChangeNumber) {
        this.lastChangeNumber = lastChangeNumber;
    }


    public Long getLastChangeNumber() {
        return lastChangeNumber;
    }

    public OffsetDateTime getLastChangedDateTime() {
        return lastChangedDateTime;
    }

    public void setLastChangedDateTime(OffsetDateTime lastChangedDateTime) {
        this.lastChangedDateTime = lastChangedDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleInstanceLastBuild that = (ScheduleInstanceLastBuild) o;
        return isEnabled() == that.isEnabled()
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getConfigScheduleKeyName(), that.getConfigScheduleKeyName())
                && Objects.equals(getLastUpdateDateTime(), that.getLastUpdateDateTime())
                && Objects.equals(getScheduleInstance(), that.getScheduleInstance())
                && Objects.equals(getLastChangedDateTime(), that.getLastChangedDateTime())
                && Objects.equals(getLastChangeNumber(), that.getLastChangeNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(),
                getConfigScheduleKeyName(),
                getScheduleInstance(),
                getLastUpdateDateTime(),
                isEnabled());
    }
}
