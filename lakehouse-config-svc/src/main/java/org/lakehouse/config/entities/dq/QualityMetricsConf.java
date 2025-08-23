package org.lakehouse.config.entities.dq;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.lakehouse.config.entities.DataSet;
import org.lakehouse.config.entities.KeyEntityAbstract;

import java.util.Objects;
@Entity
public class QualityMetricsConf extends KeyEntityAbstract {


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf__data_set__fk"))
    private DataSet dataSet;



    private boolean enabled;

    public QualityMetricsConf() {
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConf that = (QualityMetricsConf) o;
        return enabled == that.enabled
                && Objects.equals(dataSet, that.dataSet) && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),dataSet, enabled);
    }
}
