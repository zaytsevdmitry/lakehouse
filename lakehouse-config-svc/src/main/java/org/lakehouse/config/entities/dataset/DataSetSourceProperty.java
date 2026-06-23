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

package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_property_data_set_source_id_key_uk", columnNames = {
        "data_set_source_id", "key"}))
public class DataSetSourceProperty extends KeyValueAbstract {


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_source_property__data_set_source_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSetSource dataSetSource;


    public DataSetSourceProperty() {

    }

    public DataSetSource getDataSetSource() {
        return dataSetSource;
    }

    public void setDataSetSource(DataSetSource dataSetSource) {
        this.dataSetSource = dataSetSource;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DataSetSourceProperty that = (DataSetSourceProperty) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getDataSetSource(), that.getDataSetSource())
                && Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDataSetSource(), getKey(), getValue());
    }
}
