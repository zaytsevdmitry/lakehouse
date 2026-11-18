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
import org.yarina.client.api.constant.Types;

import java.util.Objects;

/**
 * Optional expectation of a threshold test set.
 *
 * <p>When expectation is absent the legacy behaviour is kept: a non-empty threshold result
 * means a violation. When expectation is present the engine extracts the measured value
 * from {@link #valueColumn}, compares it to {@link #threshold} using {@link #operator}
 * and fails only when the comparison is not satisfied.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QualityMetricsConfExpectationDTO {
    private String valueColumn = "value";
    private String operator;
    private QualityMetricsThresholdDTO threshold;
    private Types.DQThresholdViolationLevel severity;

    public QualityMetricsConfExpectationDTO() {
    }

    public String getValueColumn() {
        return valueColumn;
    }

    public void setValueColumn(String valueColumn) {
        this.valueColumn = valueColumn;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public QualityMetricsThresholdDTO getThreshold() {
        return threshold;
    }

    public void setThreshold(QualityMetricsThresholdDTO threshold) {
        this.threshold = threshold;
    }

    public Types.DQThresholdViolationLevel getSeverity() {
        return severity;
    }

    public void setSeverity(Types.DQThresholdViolationLevel severity) {
        this.severity = severity;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfExpectationDTO that = (QualityMetricsConfExpectationDTO) o;
        return Objects.equals(valueColumn, that.valueColumn)
                && Objects.equals(operator, that.operator)
                && Objects.equals(threshold, that.threshold)
                && severity == that.severity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueColumn, operator, threshold, severity);
    }

    @Override
    public String toString() {
        return "QualityMetricsConfExpectationDTO{" +
                "valueColumn='" + valueColumn + '\'' +
                ", operator='" + operator + '\'' +
                ", threshold=" + threshold +
                ", severity=" + severity +
                '}';
    }
}
