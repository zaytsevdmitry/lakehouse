/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.dataset.DataSet;

import java.util.Objects;

@Entity
public class QualityMetricsConfSource {
    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source__data_set__fk"))
    private DataSet source;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    public QualityMetricsConfSource() {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfSource that = (QualityMetricsConfSource) o;
        return Objects.equals(source, that.source) && Objects.equals(id, that.id) && Objects.equals(qualityMetricsConf, that.qualityMetricsConf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id, qualityMetricsConf);
    }
}
