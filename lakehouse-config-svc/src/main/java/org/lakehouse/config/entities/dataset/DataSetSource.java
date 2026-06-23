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

package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.dq.QualityMetricsConf;

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
