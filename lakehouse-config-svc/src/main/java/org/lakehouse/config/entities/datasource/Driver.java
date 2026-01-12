package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Entity;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class Driver extends KeyEntityAbstract {

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    private Types.DataSourceType dataSourceType;

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
