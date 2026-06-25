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
