package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Types;

import java.util.Objects;
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_name_source_name_uk", columnNames = {
        "quality_metrics_conf_key_name", "key_name" }))
public class QualityMetricsConfTestSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    private String keyName;
    private String description;

    private Types.DQMetricsType dqMetricsType;
    private boolean save;
    private String value;

    public QualityMetricsConfTestSet() {
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSet that = (QualityMetricsConfTestSet) o;
        return save == that.save && Objects.equals(id, that.id) && Objects.equals(qualityMetricsConf, that.qualityMetricsConf) && Objects.equals(keyName, that.keyName) && Objects.equals(description, that.description) && dqMetricsType == that.dqMetricsType && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, qualityMetricsConf, keyName, description, dqMetricsType, save, value);
    }
}
