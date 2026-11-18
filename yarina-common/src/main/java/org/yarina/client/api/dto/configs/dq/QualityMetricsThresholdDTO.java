/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.client.api.dto.configs.dq;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Threshold value of a DQ expectation.
 *
 * <p>May be declared either as an absolute number:
 * <pre>{"threshold": 0}</pre>
 * or as a value relative to another metric of the same QM run:
 * <pre>{"threshold": {"relativeTo": "non_zero_count", "factor": 0.5}}</pre>
 */
@JsonDeserialize(using = QualityMetricsThresholdDTODeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QualityMetricsThresholdDTO {
    private BigDecimal value;
    private String relativeTo;
    private BigDecimal factor;

    public QualityMetricsThresholdDTO() {
    }

    public QualityMetricsThresholdDTO(BigDecimal value) {
        this.value = value;
    }

    public QualityMetricsThresholdDTO(String relativeTo, BigDecimal factor) {
        this.relativeTo = relativeTo;
        this.factor = factor;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    /**
     * @return true when the threshold is declared as {relativeTo, factor}.
     */
    public boolean isRelative() {
        return relativeTo != null && !relativeTo.isBlank();
    }

    public boolean isAbsolute() {
        return value != null && !isRelative();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsThresholdDTO that = (QualityMetricsThresholdDTO) o;
        return decimalEquals(value, that.value)
                && Objects.equals(relativeTo, that.relativeTo)
                && decimalEquals(factor, that.factor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(value), relativeTo, normalize(factor));
    }

    @Override
    public String toString() {
        return isRelative()
                ? "QualityMetricsThresholdDTO{relativeTo='" + relativeTo + "', factor=" + factor + '}'
                : "QualityMetricsThresholdDTO{value=" + value + '}';
    }

    private static boolean decimalEquals(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
