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
