package org.lakehouse.config.entities.dq;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

@MappedSuperclass
public class QualityMetricsConfTestSetAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyName;
    private String description;

    private Types.DQMetricsType dqMetricsType;
    private boolean save;
    private String value;

    public QualityMetricsConfTestSetAbstract() {
    }
    public void of(QualityMetricsConfTestSetAbstract a) {
        this.id = a.getId();
        this.keyName = a.getKeyName();
        this.description = a.getDescription();
        this.dqMetricsType = a.getDqMetricsType();
        this.save = a.isSave();
        this.value = a.getValue();
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
        QualityMetricsConfTestSetAbstract that = (QualityMetricsConfTestSetAbstract) o;
        return save == that.save
                && Objects.equals(id, that.id)
                && Objects.equals(keyName, that.keyName)
                && Objects.equals(description, that.description)
                && dqMetricsType == that.dqMetricsType
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, keyName, description, dqMetricsType, save, value);
    }
}
