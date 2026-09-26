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

package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.lakehouse.config.entities.KeyEntityAbstract;
import org.lakehouse.config.entities.datasource.DataSource;

import java.util.Objects;

@Entity
public class DataSet extends KeyEntityAbstract {

    @Column(nullable = true)
    private String domainKeyName;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__data_source_fk"))
    private DataSource dataSource;


    @Column(nullable = false)
    private String databaseSchemaName;

    @Column(nullable = false)
    private String tableName;

    @Column(nullable = false)
    private boolean isVcsManaged;

    public DataSet() {
    }

    public boolean isVcsManaged() {
        return isVcsManaged;
    }

    public void setVcsManaged(boolean vcsManaged) {
        this.isVcsManaged = vcsManaged;
    }

    public String getDomainKeyName() {
        return domainKeyName;
    }

    public void setDomainKeyName(String domainKeyName) {
        this.domainKeyName = domainKeyName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataSet dataSet = (DataSet) o;
        return Objects.equals(getDomainKeyName(), dataSet.getDomainKeyName()) && Objects.equals(getDataSource(), dataSet.getDataSource()) && Objects.equals(getDatabaseSchemaName(), dataSet.getDatabaseSchemaName()) && Objects.equals(getTableName(), dataSet.getTableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDomainKeyName(), getDataSource(), getDatabaseSchemaName(), getTableName());
    }
}
