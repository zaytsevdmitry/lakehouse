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
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class DataSource extends KeyEntityAbstract {

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "data_source__driver_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Driver driver;

    @Column(nullable = false)
    private String catalogKeyName;

    public DataSource() {
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getCatalogKeyName() {
        return catalogKeyName;
    }

    public void setCatalogKeyName(String catalogKeyName) {
        this.catalogKeyName = catalogKeyName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataSource that = (DataSource) o;
        return Objects.equals(getDriver(), that.getDriver()) && Objects.equals(getCatalogKeyName(), that.getCatalogKeyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDriver(), getCatalogKeyName());
    }
}
