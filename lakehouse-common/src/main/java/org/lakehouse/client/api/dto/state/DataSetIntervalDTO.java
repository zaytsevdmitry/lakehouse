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

import org.lakehouse.client.api.dto.common.IntervalDTO;

import java.util.Objects;

public class DataSetIntervalDTO extends IntervalDTO {
    private String dataSetKeyName;

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
        return super.equals(o)
                && Objects.equals(getDataSetKeyName(), that.getDataSetKeyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getIntervalStartDateTime(), getIntervalEndDateTime(), super.hashCode());
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "{" +
                "dataSetKeyName='" + dataSetKeyName + '\'' +
                ", intervalStartDateTime='" + getIntervalStartDateTime() + '\'' +
                ", intervalEndDateTime='" + getIntervalEndDateTime() + '\'' +
                '}';
    }
}
