package org.lakehouse.client.api.dto.state;

import java.util.Objects;

public class DataSetStateDTO  extends DataSetIntervalDTO{
    String status;
    String processName;
    public DataSetStateDTO() {
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

    @Override
    public String toString() {
        return this.getClass().getName() +"{" +
                "dataSetKeyName='" + dataSetKeyName + '\'' +
                ", intervalStartDateTime='" + getIntervalStartDateTime() + '\'' +
                ", intervalEndDateTime='" + getIntervalEndDateTime() + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
