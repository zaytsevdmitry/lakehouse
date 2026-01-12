package org.lakehouse.config.entities.datasource;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;

@Entity
public class DataSource extends KeyEntityAbstract {

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "data_source__driver_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Driver driver;

    public DataSource() {
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
        DataSource that = (DataSource) o;
        return Objects.equals(driver, that.driver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), driver);
    }
}
