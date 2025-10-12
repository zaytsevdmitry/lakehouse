package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Entity;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class DataSource extends KeyEntityAbstract {

    private Types.DataSourceType dataSourceType;
    private Types.DataSourceServiceType dataSourceServiceType;


    public DataSource() {
    }


    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public Types.DataSourceServiceType getDataSourceServiceType() {
        return dataSourceServiceType;
    }

    public void setDataSourceServiceType(Types.DataSourceServiceType dataSourceServiceType) {
        this.dataSourceServiceType = dataSourceServiceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataSource that = (DataSource) o;
        return getDataSourceType() == that.getDataSourceType() && getDataSourceServiceType() == that.getDataSourceServiceType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSourceType(), getDataSourceServiceType());
    }
}
