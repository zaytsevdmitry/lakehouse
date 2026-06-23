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
