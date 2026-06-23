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

package org.lakehouse.state.entity;

import jakarta.persistence.*;
import org.lakehouse.client.api.constant.Status;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_state__uk", columnNames = {
        "data_set_key_name",
        "interval_start_date_time",
        "interval_end_date_time"}))
public class DataSetState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    String dataSetKeyName;

    @Column(nullable = false)
    OffsetDateTime intervalStartDateTime;

    @Column(nullable = false)
    OffsetDateTime intervalEndDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Status.DataSet status;

    @Column(length = 1000)
    String lockSource;

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
        DataSetState dataSetState = (DataSetState) o;
        return Objects.equals(getDataSetKeyName(), dataSetState.getDataSetKeyName())
                && Objects.equals(getIntervalStartDateTime(), dataSetState.getIntervalStartDateTime())
                && Objects.equals(getIntervalEndDateTime(), dataSetState.getIntervalEndDateTime())
                && Objects.equals(getStatus(), dataSetState.getStatus())
                && Objects.equals(getId(), dataSetState.getId())
                && Objects.equals(getLockSource(), dataSetState.getLockSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), getStatus(), getId(), getLockSource());
    }

    @Override
    public String toString() {
        return "DataSetState{" +
                "id=" + id +
                ", dataSetKeyName='" + dataSetKeyName + '\'' +
                ", intervalStartDateTime=" + intervalStartDateTime +
                ", intervalEndDateTime=" + intervalEndDateTime +
                ", status='" + status + '\'' +
                ", lockSource='" + lockSource + '\'' +
                '}';
    }


    public DataSetState copy() throws CloneNotSupportedException {
        DataSetState result = new DataSetState();
        result.setId(getId());
        result.setDataSetKeyName(getDataSetKeyName());
        result.setIntervalStartDateTime(getIntervalStartDateTime());
        result.setIntervalEndDateTime(getIntervalEndDateTime());
        result.setStatus(getStatus());
        result.setLockSource(getLockSource());
        return result;
    }
}
