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

package org.lakehouse.config.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Schedule extends KeyEntityAbstract {

    @Column(nullable = false)
    private String intervalExpression;

    @Column(nullable = false)
    private OffsetDateTime startDateTime;

    private OffsetDateTime endDateTime;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private OffsetDateTime lastChangedDateTime;

    @Column(nullable = false)
    private Long lastChangeNumber = 0L;

    public Schedule() {
    }

    public String getIntervalExpression() {
        return intervalExpression;
    }

    public void setIntervalExpression(String intervalExpression) {
        this.intervalExpression = intervalExpression;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getLastChangedDateTime() {
        return lastChangedDateTime;
    }

    public void setLastChangedDateTime(OffsetDateTime lastChangedDateTime) {
        this.lastChangedDateTime = lastChangedDateTime;
    }

    public Long getLastChangeNumber() {
        return lastChangeNumber;
    }

    public void setLastChangeNumber(Long lastChangeNumber) {
        this.lastChangeNumber = lastChangeNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Schedule schedule = (Schedule) o;
        return isEnabled() == schedule.isEnabled()
                && Objects.equals(getIntervalExpression(), schedule.getIntervalExpression())
                && Objects.equals(getStartDateTime(), schedule.getStartDateTime())
                && Objects.equals(getLastChangedDateTime(), schedule.getLastChangedDateTime())
                && Objects.equals(getEndDateTime(), schedule.getEndDateTime())
                && Objects.equals(getLastChangeNumber(), schedule.getLastChangeNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIntervalExpression(), getStartDateTime(), getEndDateTime(),
                isEnabled(), getLastChangedDateTime(), getLastChangeNumber());
    }
}
