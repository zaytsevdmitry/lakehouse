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

package org.lakehouse.config.entities.datasource;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "data_source_service_property_data_source_name_key_uk",
        columnNames = {"data_source_service_id", "key"}))
public class DataSourceSvcItemProperty extends KeyValueAbstract {
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_source_service_property__data_source_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSourceSvcItem dataSourceSvcItem;

    public DataSourceSvcItemProperty() {
    }

    public DataSourceSvcItem getDataSourceSvcItem() {
        return dataSourceSvcItem;
    }

    public void setDataSourceSvcItem(DataSourceSvcItem dataSourceSvcItem) {
        this.dataSourceSvcItem = dataSourceSvcItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        DataSourceSvcItemProperty that = (DataSourceSvcItemProperty) o;
        return Objects.equals(getDataSourceSvcItem(), that.getDataSourceSvcItem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSourceSvcItem());
    }
}
