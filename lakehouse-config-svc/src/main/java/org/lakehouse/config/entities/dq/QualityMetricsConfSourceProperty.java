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
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "quality_metrics_conf_source_property_source_id_key_uk", columnNames = {
        "data_set_source_id", "key"}))
public class QualityMetricsConfSourceProperty extends KeyValueAbstract {


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_source_property_source__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConfSource qualityMetricsConfSource;


    public QualityMetricsConfSourceProperty() {
    }

    public QualityMetricsConfSource getQualityMetricsConfSource() {
        return qualityMetricsConfSource;
    }

    public void setQualityMetricsConfSource(QualityMetricsConfSource qualityMetricsConfSource) {
        this.qualityMetricsConfSource = qualityMetricsConfSource;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QualityMetricsConfSourceProperty that = (QualityMetricsConfSourceProperty) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getQualityMetricsConfSource(), that.getQualityMetricsConfSource())
                && Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getQualityMetricsConfSource(), getKey(), getValue());
    }
}
