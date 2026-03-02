package org.lakehouse.client.api.dto.configs.dq;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QualityMetricsConfTestSetDTO {
    private String description;
    private Types.DQMetricTestSetType type;
    private boolean save;
    private List<ScriptReferenceDTO> scripts = new ArrayList<>();

    public QualityMetricsConfTestSetDTO() {
    }

    public Types.DQMetricTestSetType getType() {
        return type;
    }

    public void setType(Types.DQMetricTestSetType type) {
        this.type = type;
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
                ", type=" + type +
                ", save=" + save +
                ", scripts=" + scripts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSetDTO that = (QualityMetricsConfTestSetDTO) o;
        return isSave() == that.isSave() &&  Objects.equals(getDescription(), that.getDescription()) && getType() == that.getType() && Objects.equals(getScripts(), that.getScripts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getType(), isSave(), getScripts());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
