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
