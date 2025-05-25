package org.lakehouse.state.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_state__uk", columnNames = {
        "data_set_key_name",
        "interval_start_date_time",
        "interval_end_date_time" }))
public class DataSetState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    String dataSetKeyName;
    OffsetDateTime intervalStartDateTime;
    OffsetDateTime intervalEndDateTime;
    String status;

    public DataSetState() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }

    public OffsetDateTime getIntervalStartDateTime() {
        return intervalStartDateTime;
    }

    public void setIntervalStartDateTime(OffsetDateTime intervalStartDateTime) {
        this.intervalStartDateTime = intervalStartDateTime;
    }

    public OffsetDateTime getIntervalEndDateTime() {
        return intervalEndDateTime;
    }

    public void setIntervalEndDateTime(OffsetDateTime intervalEndDateTime) {
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
        DataSetState dataSetState = (DataSetState) o;
        return Objects.equals(getDataSetKeyName(), dataSetState.getDataSetKeyName())
                && Objects.equals(getIntervalStartDateTime(), dataSetState.getIntervalStartDateTime())
                && Objects.equals(getIntervalEndDateTime(), dataSetState.getIntervalEndDateTime())
                && Objects.equals(getStatus(), dataSetState.getStatus())
                && Objects.equals(getId(), dataSetState.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), getStatus(), getId());
    }

    @Override
    public String toString() {
        return "DataSetState{" +
                "id=" + id +
                ", dataSetKeyName='" + dataSetKeyName + '\'' +
                ", intervalStartDateTime=" + intervalStartDateTime +
                ", intervalEndDateTime=" + intervalEndDateTime +
                ", status='" + status + '\'' +
                '}';
    }
}
