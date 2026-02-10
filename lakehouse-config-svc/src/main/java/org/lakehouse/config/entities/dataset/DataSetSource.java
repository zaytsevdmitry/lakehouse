package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_key_name_source_key_name_uk", columnNames = {
        "data_set_key_name", "source_key_name", "quality_metrics_conf_key_name"}))
public class DataSetSource {

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_source__data_set_cur_fk"))
    private DataSet source;
    // pk
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_source__data_set_src_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSet dataSet;

    @ManyToOne(optional = true)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_source__quality_metrics_conf_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    QualityMetricsConf qualityMetricsConf;

    public DataSetSource() {
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
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
        if (o == null || getClass() != o.getClass()) return false;
        DataSetSource that = (DataSetSource) o;
        return Objects.equals(getSource(), that.getSource()) && Objects.equals(getId(), that.getId()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getQualityMetricsConf(), that.getQualityMetricsConf());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getId(), getDataSet(), getQualityMetricsConf());
    }

    @Override
    public String toString() {
        return "DataSetSource{" +
                "source=" + source +
                ", id=" + id +
                ", dataSet=" + dataSet +
                ", qualityMetricsConf=" + qualityMetricsConf +
                '}';
    }
}
