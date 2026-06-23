/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.config.entities.script;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "data_set_script_data_set_key_name__script_key_uk",
                columnNames = {"data_set_key_name", "script_key"}),
        @UniqueConstraint(
                name = "data_set_script_quality_metrics_conf_test_set_id__script_key_uk",
                columnNames = {"quality_metrics_conf_test_set_id", "script_key"})
        }

)
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
