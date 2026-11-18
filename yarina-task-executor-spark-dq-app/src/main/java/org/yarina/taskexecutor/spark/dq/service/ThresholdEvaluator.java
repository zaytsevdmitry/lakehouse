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

package org.yarina.taskexecutor.spark.dq.service;

import org.yarina.client.api.constant.Types;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Pure comparison logic for DQ expectations. Kept free of Spark dependencies so it can be
 * unit tested and reused by different runners.
 */
public final class ThresholdEvaluator {

    private ThresholdEvaluator() {
    }

    public static boolean matches(BigDecimal measured, String operator, BigDecimal threshold) {
        if (measured == null) {
            // A test set that did not return a value cannot satisfy an expectation.
            return false;
        }
        if (threshold == null) {
            throw new IllegalArgumentException("expectation.threshold must be defined");
        }
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("expectation.operator must be defined");
        }

        int comparison = measured.compareTo(threshold);
        return switch (operator.trim()) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            case "=", "==" -> comparison == 0;
            case "!=", "<>" -> comparison != 0;
            default -> throw new IllegalArgumentException(
                    "Unsupported expectation operator '" + operator + "'. Supported: >, >=, <, <=, =, !=");
        };
    }

    public static BigDecimal relativeThreshold(BigDecimal baseline, BigDecimal factor) {
        if (baseline == null) {
            throw new IllegalArgumentException("relative threshold baseline value is not available");
        }
        if (factor == null) {
            throw new IllegalArgumentException("relative threshold factor must be defined");
        }
        return baseline.multiply(factor);
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : new BigDecimal(trimmed);
        }
        throw new IllegalArgumentException("Value '" + value + "' of type " + value.getClass().getName() + " is not numeric");
    }

    /**
     * Falls back to the QM-level violation level when an expectation does not define severity.
     */
    public static Types.DQThresholdViolationLevel severity(
            Types.DQThresholdViolationLevel expectationSeverity,
            Types.DQThresholdViolationLevel qmSeverity) {
        if (expectationSeverity != null) {
            return expectationSeverity;
        }
        return qmSeverity == null ? Types.DQThresholdViolationLevel.ERROR : qmSeverity;
    }

    public static boolean isError(Types.DQThresholdViolationLevel severity) {
        // INFO is a legacy non-blocking level, keep it equivalent to WARN.
        return severity == null || severity == Types.DQThresholdViolationLevel.ERROR;
    }

    public static Types.DQThresholdViolationLevel parseSeverity(String value, Types.DQThresholdViolationLevel fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (Types.DQThresholdViolationLevel level : Types.DQThresholdViolationLevel.values()) {
            if (level.name().equals(normalized) || level.label.equalsIgnoreCase(value.trim())) {
                return level;
            }
        }
        return fallback;
    }
}
