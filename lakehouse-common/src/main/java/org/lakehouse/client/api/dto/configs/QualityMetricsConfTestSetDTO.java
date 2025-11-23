package org.lakehouse.client.api.dto.configs;

import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

public class QualityMetricsConfTestSetDTO {
    private String keyName;
    private String description;
    private Types.DQMetricsType dqMetricsType;
    private boolean save;
    private String value;

    public QualityMetricsConfTestSetDTO() {
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
    public String toString() {
        return "QualityMetricsConfTestSetDTO{" +
                "keyName='" + keyName + '\'' +
                ", description='" + description + '\'' +
                ", dqMetricsType=" + dqMetricsType +
                ", save=" + save +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSetDTO that = (QualityMetricsConfTestSetDTO) o;
        return save == that.save && Objects.equals(keyName, that.keyName) && Objects.equals(description, that.description) && dqMetricsType == that.dqMetricsType && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, description, dqMetricsType, save, value);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
