package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;

import java.util.Objects;

@Entity
public class SQLTemplate extends KeyValueAbstract {
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__data_source_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSource dataSource;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__data_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSet dataSet;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__driver_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Driver driver;

    public SQLTemplate() {
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SQLTemplate that = (SQLTemplate) o;
        return Objects.equals(getDataSource(), that.getDataSource()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getDriver(), that.getDriver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSource(), getDataSet(), getDriver());
    }
}
