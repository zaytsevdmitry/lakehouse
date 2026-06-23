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
@Table(uniqueConstraints = @UniqueConstraint(name = "schedule_instance_running_schedule_name_uk", columnNames = {
        "config_schedule_key_name"}))
public class ScheduleInstanceRunning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String configScheduleKeyName;

    @OneToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ScheduleInstance scheduleInstance;

    public ScheduleInstanceRunning() {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleInstanceRunning that = (ScheduleInstanceRunning) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getConfigScheduleKeyName(), that.getConfigScheduleKeyName())
                && Objects.equals(getScheduleInstance(), that.getScheduleInstance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getConfigScheduleKeyName(), getScheduleInstance());
    }
}
