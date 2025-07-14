package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.DataSet;

import java.util.Objects;

public class QualityMetricsConfSource {
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source__data_set__fk"))
    private DataSet source;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    public QualityMetricsConfSource() {
    }

    public DataSet getSource() {
        return source;
    }

    public void setSource(DataSet source) {
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        QualityMetricsConfSource that = (QualityMetricsConfSource) o;
        return Objects.equals(source, that.source) && Objects.equals(id, that.id) && Objects.equals(qualityMetricsConf, that.qualityMetricsConf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id, qualityMetricsConf);
    }
}
