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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class Driver extends KeyEntityAbstract {


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Types.DataSourceType dataSourceType;

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Driver driver = (Driver) o;
        return getDataSourceType() == driver.getDataSourceType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSourceType());
    }
}
