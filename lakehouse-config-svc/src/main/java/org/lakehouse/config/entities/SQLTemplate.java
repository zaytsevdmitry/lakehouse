package org.lakehouse.config.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.entities.script.Script;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "sql_template_driver_datasource_dataset_key_uk", columnNames = {"driver_key_name", "data_source_key_name", "data_set_key_name", "key"})
}
)
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

    @Column(nullable = false, length = 4000)
    @ManyToOne(targetEntity = Script.class, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "sql_template__script_fk"))
    private String value;

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
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        this.value = value;
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
