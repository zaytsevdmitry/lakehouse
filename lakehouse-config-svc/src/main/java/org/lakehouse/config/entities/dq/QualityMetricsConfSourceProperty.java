package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "quality_metrics_conf_source_property_source_id_key_uk", columnNames = {
        "data_set_source_id", "key"}))
public class QualityMetricsConfSourceProperty extends KeyValueAbstract {


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source_property_source__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConfSource qualityMetricsConfSource;


    public QualityMetricsConfSourceProperty() {
    }

    public QualityMetricsConfSource getQualityMetricsConfSource() {
        return qualityMetricsConfSource;
    }

    public void setQualityMetricsConfSource(QualityMetricsConfSource qualityMetricsConfSource) {
        this.qualityMetricsConfSource = qualityMetricsConfSource;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QualityMetricsConfSourceProperty that = (QualityMetricsConfSourceProperty) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getQualityMetricsConfSource(), that.getQualityMetricsConfSource())
                && Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getQualityMetricsConfSource(), getKey(), getValue());
    }
}
