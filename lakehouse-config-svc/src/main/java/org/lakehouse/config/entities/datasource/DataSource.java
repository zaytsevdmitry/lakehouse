package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Entity;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class DataSource extends KeyEntityAbstract {

    private Types.EngineType engineType;
    private Types.Engine engine;


    public DataSource() {
    }


    public Types.EngineType getDataSourceType() {
        return engineType;
    }

    public void setDataSourceType(Types.EngineType engineType) {
        this.engineType = engineType;
    }

    public Types.Engine getDataSourceServiceType() {
        return engine;
    }

    public void setDataSourceServiceType(Types.Engine engine) {
        this.engine = engine;
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
