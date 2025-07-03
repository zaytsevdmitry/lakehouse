package org.lakehouse.client.api.dto.state;

import java.util.Objects;

public class DataSetStateDTO  extends DataSetIntervalDTO{
    String status;
    String lockHash;
    public DataSetStateDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLockHash() {
        return lockHash;
    }

    public void setLockHash(String lockHash) {
        this.lockHash = lockHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetStateDTO that = (DataSetStateDTO) o;
        return Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getLockHash(), that.getLockHash())
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), getStatus(), super.hashCode(), getLockHash());
    }

    @Override
    public String toString() {
        return "DataSetStateDTO{" +
                "status='" + status + '\'' +
                ", lockHash='" + lockHash + '\'' +
                ", dataSetKeyName='" + dataSetKeyName + '\'' +
                '}';
    }
}
