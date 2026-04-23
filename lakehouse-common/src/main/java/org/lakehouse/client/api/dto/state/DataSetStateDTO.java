package org.lakehouse.client.api.dto.state;

import org.lakehouse.client.api.constant.Status;

import java.util.Objects;

public class DataSetStateDTO extends DataSetIntervalDTO {
    Status.DataSet status;
    String lockSource;

    public DataSetStateDTO() {
    }

    public Status.DataSet getStatus() {
        return status;
    }

    public void setStatus(Status.DataSet status) {
        this.status = status;
    }

    public String getLockSource() {
        return lockSource;
    }

    public void setLockSource(String lockSource) {
        this.lockSource = lockSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetStateDTO that = (DataSetStateDTO) o;
        return Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getLockSource(), that.getLockSource())
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), getStatus(), super.hashCode(), getLockSource());
    }

    @Override
    public String toString() {
        return "DataSetStateDTO{" +
                "status='" + status + '\'' +
                ", lockSource='" + lockSource + '\'' +
                ", dataSetKeyName='" + getDataSetKeyName() + '\'' +
                ", intervalStartDateTime='" + getIntervalStartDateTime() + '\'' +
                ", intervalEndDateTime='" + getIntervalEndDateTime() + '\'' +
                '}';
    }
}
