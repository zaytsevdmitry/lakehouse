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

package org.lakehouse.client.api.dto.scheduler;

import org.lakehouse.client.api.constant.Status;

import java.util.Objects;

public class ScheduleInstanceDTO {
    private Long id;

    private String configScheduleKeyName;

    private String targetExecutionDateTime;

    private Status.Schedule status;

    public ScheduleInstanceDTO() {
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

    public String getTargetExecutionDateTime() {
        return targetExecutionDateTime;
    }

    public void setTargetExecutionDateTime(String targetExecutionDateTime) {
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
        ScheduleInstanceDTO that = (ScheduleInstanceDTO) o;
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
