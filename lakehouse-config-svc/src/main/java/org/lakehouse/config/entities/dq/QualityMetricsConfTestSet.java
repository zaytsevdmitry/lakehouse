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
import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_name_source_name_uk", columnNames = {
        "quality_metrics_conf_key_name", "key_name"}))
public class QualityMetricsConfTestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf_test_set__quality_metrics_conf__fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QualityMetricsConf qualityMetricsConf;

    @Column(nullable = false) private String keyName;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private Types.DQMetricTestSetType dqMetricTestSetType;

    @Column(nullable = false) @Enumerated(EnumType.ORDINAL) private ElementType elementType;
    public QualityMetricsConfTestSet() {
    }

    public void of(QualityMetricsConfTestSet a) {
        this.id = a.getId();
        this.keyName = a.getKeyName();
        this.description = a.getDescription();
        this.dqMetricTestSetType = a.getDqMetricsType();
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

    public Types.DQMetricTestSetType getDqMetricsType() {
        return dqMetricTestSetType;
    }

    public void setDqMetricsType(Types.DQMetricTestSetType dqMetricTestSetType) {
        this.dqMetricTestSetType = dqMetricTestSetType;
    }



    public QualityMetricsConf getQualityMetricsConf() {
        return qualityMetricsConf;
    }

    public void setQualityMetricsConf(QualityMetricsConf qualityMetricsConf) {
        this.qualityMetricsConf = qualityMetricsConf;
    }

    public Types.DQMetricTestSetType getDqMetricTestSetType() {
        return dqMetricTestSetType;
    }

    public void setDqMetricTestSetType(Types.DQMetricTestSetType dqMetricTestSetType) {
        this.dqMetricTestSetType = dqMetricTestSetType;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSet testSet = (QualityMetricsConfTestSet) o;
        return  Objects.equals(getId(), testSet.getId()) && Objects.equals(getQualityMetricsConf(), testSet.getQualityMetricsConf()) && Objects.equals(getKeyName(), testSet.getKeyName()) && Objects.equals(getDescription(), testSet.getDescription()) && getDqMetricTestSetType() == testSet.getDqMetricTestSetType() && getElementType() == testSet.getElementType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getQualityMetricsConf(), getKeyName(), getDescription(), getDqMetricTestSetType(), getElementType());
    }
}
