package org.lakehouse.client.api.dto.state;

import java.util.Objects;

public class DataSetStateDTO {
    String dataSetKeyName;
    String intervalStartDateTime;
    String intervalEndDateTime;
    String status;

    public DataSetStateDTO() {
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetStateDTO that = (DataSetStateDTO) o;
        return Objects.equals(getDataSetKeyName(), that.getDataSetKeyName())
                && Objects.equals(getIntervalStartDateTime(), that.getIntervalStartDateTime())
                && Objects.equals(getIntervalEndDateTime(), that.getIntervalEndDateTime())
                && Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), getStatus());
    }
}
