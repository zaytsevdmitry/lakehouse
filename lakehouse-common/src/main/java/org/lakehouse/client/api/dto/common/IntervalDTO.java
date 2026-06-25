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
