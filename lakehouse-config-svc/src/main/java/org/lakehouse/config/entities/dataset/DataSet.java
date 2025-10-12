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
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__data_store_fk"))
    private DataSource dataSource;

    @Column(nullable = false)
    private String fullTableName;

    public DataSet() {
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(NameSpace nameSpace) {
        this.nameSpace = nameSpace;
    }

    public DataSource getDataStore() {
        return dataSource;
    }

    public void setDataStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getFullTableName() {
        return fullTableName;
    }

    public void setFullTableName(String fullTableName) {
        this.fullTableName = fullTableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        DataSet dataSet = (DataSet) o;
        return Objects.equals(getNameSpace(), dataSet.getNameSpace())
                && Objects.equals(getDataStore(), dataSet.getDataStore())
                && Objects.equals(getFullTableName(), dataSet.getFullTableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNameSpace(), getDataStore(), getFullTableName());
    }
}
