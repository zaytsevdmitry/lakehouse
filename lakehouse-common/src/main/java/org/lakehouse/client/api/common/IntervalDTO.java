package org.lakehouse.client.api.common;

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
