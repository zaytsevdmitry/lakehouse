package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class Driver extends KeyEntityAbstract {


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Types.DataSourceType dataSourceType;

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Driver driver = (Driver) o;
        return getDataSourceType() == driver.getDataSourceType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSourceType());
    }
}
