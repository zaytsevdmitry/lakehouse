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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.lakehouse.client.api.constant.DatabaseProtocol;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class DataSource extends KeyEntityAbstract {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    private Types.DataSourceType dataSourceType;
    @Enumerated(EnumType.STRING)
    @Column
    private DatabaseProtocol databaseProtocol;

    @Column(nullable = false)
    private boolean isVcsManaged;

    public DataSource() {
    }

    public boolean isVcsManaged() {
        return isVcsManaged;
    }

    public void setVcsManaged(boolean vcsManaged) {
        this.isVcsManaged = vcsManaged;
    }

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public DatabaseProtocol getDatabaseProtocol() {
        return databaseProtocol;
    }

    public void setDatabaseProtocol(DatabaseProtocol databaseProtocol) {
        this.databaseProtocol = databaseProtocol;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataSource that = (DataSource) o;
        return getDataSourceType() == that.getDataSourceType() && getDatabaseProtocol() == that.getDatabaseProtocol();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSourceType(), getDatabaseProtocol());
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "dataSourceType=" + dataSourceType +
                ", databaseProtocol=" + databaseProtocol +
                '}';
    }
}
