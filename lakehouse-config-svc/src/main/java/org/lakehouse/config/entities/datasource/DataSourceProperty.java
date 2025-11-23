package org.lakehouse.config.entities.datasource;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_source_property_data_source_name_key_uk", columnNames = {
        "data_source_name", "key"}))
public class DataSourceProperty extends KeyValueAbstract {
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_source_property__data_source_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSource dataSource;

    public DataSourceProperty() {
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        DataSourceProperty that = (DataSourceProperty) o;
        return Objects.equals(getDataSource(), that.getDataSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSource());
    }
}
