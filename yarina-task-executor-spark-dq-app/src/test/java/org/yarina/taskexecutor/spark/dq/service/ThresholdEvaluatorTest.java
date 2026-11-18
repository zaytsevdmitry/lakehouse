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

import org.junit.jupiter.api.Test;
import org.yarina.client.api.constant.Types;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThresholdEvaluatorTest {

    private static final BigDecimal TEN = BigDecimal.TEN;

    @Test
    void absoluteOperators() {
        assertTrue(ThresholdEvaluator.matches(TEN, ">", BigDecimal.ONE));
        assertFalse(ThresholdEvaluator.matches(TEN, ">", TEN));
        assertTrue(ThresholdEvaluator.matches(TEN, ">=", TEN));
        assertTrue(ThresholdEvaluator.matches(TEN, "<", new BigDecimal("11")));
        assertFalse(ThresholdEvaluator.matches(TEN, "<", TEN));
        assertTrue(ThresholdEvaluator.matches(TEN, "<=", TEN));
        assertTrue(ThresholdEvaluator.matches(TEN, "=", new BigDecimal("10.0")));
        assertFalse(ThresholdEvaluator.matches(TEN, "=", BigDecimal.ONE));
        assertTrue(ThresholdEvaluator.matches(TEN, "==", TEN));
        assertTrue(ThresholdEvaluator.matches(TEN, "!=", BigDecimal.ONE));
        assertTrue(ThresholdEvaluator.matches(TEN, "<>", BigDecimal.ONE));
    }

    @Test
    void trimsOperator() {
        assertTrue(ThresholdEvaluator.matches(TEN, " >= ", TEN));
    }

    @Test
    void rejectsUnsupportedOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.matches(TEN, "~", BigDecimal.ONE));
    }

    @Test
    void missingMeasuredValueDoesNotSatisfyExpectation() {
        assertFalse(ThresholdEvaluator.matches(null, "=", BigDecimal.ZERO));
    }

    @Test
    void missingThresholdOrOperatorIsConfigurationError() {
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.matches(TEN, "=", null));
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.matches(TEN, null, TEN));
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.matches(TEN, "  ", TEN));
    }

    @Test
    void relativeThreshold() {
        BigDecimal threshold = ThresholdEvaluator.relativeThreshold(
                new BigDecimal("100"), new BigDecimal("0.5"));
        assertEquals(0, threshold.compareTo(new BigDecimal("50")));
        assertTrue(ThresholdEvaluator.matches(new BigDecimal("60"), ">=", threshold));
        assertFalse(ThresholdEvaluator.matches(new BigDecimal("40"), ">=", threshold));
    }

    @Test
    void relativeThresholdRequiresBaselineAndFactor() {
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.relativeThreshold(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.relativeThreshold(BigDecimal.ONE, null));
    }

    @Test
    void convertsNumbersAndStringsToBigDecimal() {
        assertEquals(new BigDecimal("42"), ThresholdEvaluator.toBigDecimal(42));
        assertEquals(new BigDecimal("42"), ThresholdEvaluator.toBigDecimal(42L));
        assertEquals(new BigDecimal("42"), ThresholdEvaluator.toBigDecimal(" 42 "));
        assertEquals(0, ThresholdEvaluator.toBigDecimal(0.5).compareTo(new BigDecimal("0.5")));
        assertNull(ThresholdEvaluator.toBigDecimal(null));
        assertNull(ThresholdEvaluator.toBigDecimal("  "));
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdEvaluator.toBigDecimal(new Object()));
    }

    @Test
    void severityFallsBackToQmLevel() {
        assertEquals(Types.DQThresholdViolationLevel.WARN,
                ThresholdEvaluator.severity(Types.DQThresholdViolationLevel.WARN,
                        Types.DQThresholdViolationLevel.ERROR));
        assertEquals(Types.DQThresholdViolationLevel.ERROR,
                ThresholdEvaluator.severity(null, Types.DQThresholdViolationLevel.ERROR));
        assertEquals(Types.DQThresholdViolationLevel.ERROR,
                ThresholdEvaluator.severity(null, null));
    }

    @Test
    void onlyErrorBlocksPipeline() {
        assertTrue(ThresholdEvaluator.isError(Types.DQThresholdViolationLevel.ERROR));
        assertTrue(ThresholdEvaluator.isError(null));
        assertFalse(ThresholdEvaluator.isError(Types.DQThresholdViolationLevel.WARN));
        // legacy INFO is non-blocking
        assertFalse(ThresholdEvaluator.isError(Types.DQThresholdViolationLevel.INFO));
    }

    @Test
    void parsesSeverityStrings() {
        assertEquals(Types.DQThresholdViolationLevel.ERROR,
                ThresholdEvaluator.parseSeverity("ERROR", Types.DQThresholdViolationLevel.WARN));
        assertEquals(Types.DQThresholdViolationLevel.WARN,
                ThresholdEvaluator.parseSeverity("warn", Types.DQThresholdViolationLevel.ERROR));
        assertEquals(Types.DQThresholdViolationLevel.INFO,
                ThresholdEvaluator.parseSeverity("Info", Types.DQThresholdViolationLevel.ERROR));
        assertEquals(Types.DQThresholdViolationLevel.ERROR,
                ThresholdEvaluator.parseSeverity(null, Types.DQThresholdViolationLevel.ERROR));
        assertEquals(Types.DQThresholdViolationLevel.ERROR,
                ThresholdEvaluator.parseSeverity("unknown", Types.DQThresholdViolationLevel.ERROR));
    }
}
