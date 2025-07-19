package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_name_source_name_uk", columnNames = {
        "quality_metrics_conf_key_name", "key_name" }))
public class QualityMetricsConfTestSetThreshold extends QualityMetricsConfTestSetAbstract{
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    public QualityMetricsConfTestSetThreshold() {
    }


    public QualityMetricsConf getQualityMetricsConf() {
        return qualityMetricsConf;
    }

    public void setQualityMetricsConf(QualityMetricsConf qualityMetricsConf) {
        this.qualityMetricsConf = qualityMetricsConf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        QualityMetricsConfTestSetThreshold that = (QualityMetricsConfTestSetThreshold) o;
        return Objects.equals(getQualityMetricsConf(), that.getQualityMetricsConf());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getQualityMetricsConf());
    }
}
