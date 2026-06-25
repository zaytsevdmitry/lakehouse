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
