package org.lakehouse.client.api.dto.configs.dq;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;

import java.util.List;
import java.util.Objects;

public class QualityMetricsConfTestSetDTO {
    private String description;
    private Types.DQMetricsType dqMetricsType;
    private boolean save;
    private List<ScriptReferenceDTO> scripts;

    public QualityMetricsConfTestSetDTO() {
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

    public List<ScriptReferenceDTO> getScripts() {
        return scripts;
    }

    public void setScripts(List<ScriptReferenceDTO> scripts) {
        this.scripts = scripts;
    }

    @Override
    public String toString() {
        return "QualityMetricsConfTestSetDTO{" +
                ", description='" + description + '\'' +
                ", dqMetricsType=" + dqMetricsType +
                ", save=" + save +
                ", scripts=" + scripts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSetDTO that = (QualityMetricsConfTestSetDTO) o;
        return isSave() == that.isSave() &&  Objects.equals(getDescription(), that.getDescription()) && getDqMetricsType() == that.getDqMetricsType() && Objects.equals(getScripts(), that.getScripts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getDqMetricsType(), isSave(), getScripts());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
