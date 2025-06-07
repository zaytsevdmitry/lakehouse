package org.lakehouse.client.api.dto.state;

import org.lakehouse.client.api.common.IntervalDTO;

import java.util.Objects;

public class DataSetIntervalDTO extends IntervalDTO {
    String dataSetKeyName;

    public DataSetIntervalDTO() {
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetIntervalDTO that = (DataSetIntervalDTO) o;
        return Objects.equals(getDataSetKeyName(), that.getDataSetKeyName())
                && Objects.equals(getIntervalStartDateTime(), that.getIntervalStartDateTime())
                && Objects.equals(getIntervalEndDateTime(), that.getIntervalEndDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime());
    }

    @Override
    public String toString() {
        return this.getClass().getName() +"{" +
                "dataSetKeyName='" + dataSetKeyName + '\'' +
                ", intervalStartDateTime='" + getIntervalStartDateTime() + '\'' +
                ", intervalEndDateTime='" + getIntervalEndDateTime() + '\'' +
                '}';
    }
}
