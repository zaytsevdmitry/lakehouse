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
