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

package org.lakehouse.client.api.dto.common;

import org.lakehouse.client.api.utils.DateTimeUtils;

import java.util.Objects;

public class IntervalDTO {
    String intervalStartDateTime;
    String intervalEndDateTime;

    public IntervalDTO() {
    }

    public String getIntervalStartDateTime() {
        return intervalStartDateTime;
    }

    public void setIntervalStartDateTime(String intervalStartDateTime) {
        this.intervalStartDateTime = intervalStartDateTime;
    }

    public String getIntervalEndDateTime() {
        return intervalEndDateTime;
    }

    public void setIntervalEndDateTime(String intervalEndDateTime) {
        this.intervalEndDateTime = intervalEndDateTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntervalDTO that = (IntervalDTO) o;
        return DateTimeUtils.strEquals(getIntervalStartDateTime(), that.getIntervalStartDateTime())
                && DateTimeUtils.strEquals(getIntervalEndDateTime(), that.getIntervalEndDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIntervalStartDateTime(), getIntervalEndDateTime());
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "{" +
                ", intervalStartDateTime='" + intervalStartDateTime + '\'' +
                ", intervalEndDateTime='" + intervalEndDateTime + '\'' +
                '}';
    }
}
