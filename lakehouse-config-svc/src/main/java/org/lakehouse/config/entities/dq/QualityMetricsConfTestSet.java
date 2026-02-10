package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;

import java.util.List;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_name_source_name_uk", columnNames = {
        "quality_metrics_conf_key_name", "key_name"}))
public class QualityMetricsConfTestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    @Column(nullable = false) private String keyName;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private Types.DQMetricsType dqMetricsType;
    @Column(nullable = false) private boolean save;
    @Column(nullable = false) private boolean isThreshold;

    public QualityMetricsConfTestSet() {
    }

    public void of(QualityMetricsConfTestSet a) {
        this.id = a.getId();
        this.keyName = a.getKeyName();
        this.description = a.getDescription();
        this.dqMetricsType = a.getDqMetricsType();
        this.save = a.isSave();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Types.DQMetricsType getDqMetricsType() {
        return dqMetricsType;
    }

    public void setDqMetricsType(Types.DQMetricsType dqMetricsType) {
        this.dqMetricsType = dqMetricsType;
    }

    public boolean isSave() {
        return save;
    }

    public void setSave(boolean save) {
        this.save = save;
    }

    public QualityMetricsConf getQualityMetricsConf() {
        return qualityMetricsConf;
    }

    public void setQualityMetricsConf(QualityMetricsConf qualityMetricsConf) {
        this.qualityMetricsConf = qualityMetricsConf;
    }

    public boolean isThreshold() {
        return isThreshold;
    }

    public void setThreshold(boolean threshold) {
        isThreshold = threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSet testSet = (QualityMetricsConfTestSet) o;
        return isSave() == testSet.isSave() && isThreshold() == testSet.isThreshold() && Objects.equals(getId(), testSet.getId()) && Objects.equals(getQualityMetricsConf(), testSet.getQualityMetricsConf()) && Objects.equals(getKeyName(), testSet.getKeyName()) && Objects.equals(getDescription(), testSet.getDescription()) && getDqMetricsType() == testSet.getDqMetricsType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getQualityMetricsConf(), getKeyName(), getDescription(), getDqMetricsType(), isSave(), isThreshold());
    }
}
