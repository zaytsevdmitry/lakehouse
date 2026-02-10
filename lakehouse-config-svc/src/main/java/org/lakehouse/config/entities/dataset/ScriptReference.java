package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.Script;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_script_data_set_key_name_source_name_uk", columnNames = {
        "data_set_key_name", "script_key"}))
public class ScriptReference {
    // pk
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // strong kf
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "script_reference__script_fk"))
    Script script;

    // optional fk
    @ManyToOne(optional = true)
    @JoinColumn(foreignKey = @ForeignKey(name = "script_reference__data_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSet dataSet;

    @ManyToOne(optional = true)
    @JoinColumn(foreignKey = @ForeignKey(name = "script_reference__quality_metrics_conf_test_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConfTestSet qualityMetricsConfTestSet;

    private Integer scriptOrder;

    public ScriptReference() {
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public Integer getScriptOrder() {
        return scriptOrder;
    }

    public void setScriptOrder(Integer scriptOrder) {
        this.scriptOrder = scriptOrder;
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
        ScriptReference that = (ScriptReference) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getScript(), that.getScript()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getQualityMetricsConfTestSet(), that.getQualityMetricsConfTestSet()) && Objects.equals(getScriptOrder(), that.getScriptOrder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScript(), getDataSet(), getQualityMetricsConfTestSet(), getScriptOrder());
    }
}
