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
import org.lakehouse.config.entities.KeyEntityAbstract;
import org.lakehouse.config.entities.NameSpace;
import org.lakehouse.config.entities.datasource.DataSource;

import java.util.Objects;

@Entity
public class DataSet extends KeyEntityAbstract {

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__nameSpace_fk"))
    private NameSpace nameSpace;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__data_source_fk"))
    private DataSource dataSource;


    @Column(nullable = false)
    private String databaseSchemaName;

    @Column(nullable = false)
    private String tableName;

    public DataSet() {
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(NameSpace nameSpace) {
        this.nameSpace = nameSpace;
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
        return Objects.equals(getNameSpace(), dataSet.getNameSpace()) && Objects.equals(getDataSource(), dataSet.getDataSource()) && Objects.equals(getDatabaseSchemaName(), dataSet.getDatabaseSchemaName()) && Objects.equals(getTableName(), dataSet.getTableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNameSpace(), getDataSource(), getDatabaseSchemaName(), getTableName());
    }
}
