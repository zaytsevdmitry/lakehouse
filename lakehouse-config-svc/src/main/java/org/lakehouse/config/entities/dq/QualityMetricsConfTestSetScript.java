package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.Script;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "quality_metrics_conf_test_set_script__uk", columnNames = {
        "data_set_key_name", "script_key"}))

public class QualityMetricsConfTestSetScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer scriptOrder;


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set_script__script_fk"))
    Script script;


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set_script__quality_metrics_conf_test_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConfTestSet qualityMetricsConfTestSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getScriptOrder() {
        return scriptOrder;
    }

    public void setScriptOrder(Integer scriptOrder) {
        this.scriptOrder = scriptOrder;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public QualityMetricsConfTestSet getQualityMetricsConfTestSet() {
        return qualityMetricsConfTestSet;
    }

    public void setQualityMetricsConfTestSet(QualityMetricsConfTestSet qualityMetricsConfTestSet) {
        this.qualityMetricsConfTestSet = qualityMetricsConfTestSet;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSetScript that = (QualityMetricsConfTestSetScript) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getScriptOrder(), that.getScriptOrder()) && Objects.equals(getScript(), that.getScript()) && Objects.equals(getQualityMetricsConfTestSet(), that.getQualityMetricsConfTestSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScriptOrder(), getScript(), getQualityMetricsConfTestSet());
    }
}
