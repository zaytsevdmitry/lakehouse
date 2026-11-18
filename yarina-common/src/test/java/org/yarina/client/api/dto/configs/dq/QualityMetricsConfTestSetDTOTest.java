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

import org.junit.jupiter.api.Test;
import org.yarina.client.api.constant.Types;
import org.yarina.client.api.utils.ObjectMapping;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QualityMetricsConfTestSetDTOTest {

    private QualityMetricsConfTestSetDTO read(String json) throws Exception {
        return ObjectMapping.stringToObject(json, QualityMetricsConfTestSetDTO.class);
    }

    @Test
    void readsAbsoluteThreshold() throws Exception {
        QualityMetricsConfTestSetDTO testSet = read("""
                {
                  "description": "commission must be numeric",
                  "type": "sparkSQL",
                  "scripts": [{"key": "dq.commission_is_numeric.sql"}],
                  "expectation": {
                    "valueColumn": "value",
                    "operator": "=",
                    "threshold": 0,
                    "severity": "error"
                  }
                }
                """);

        assertNotNull(testSet.getExpectation());
        assertEquals("value", testSet.getExpectation().getValueColumn());
        assertEquals("=", testSet.getExpectation().getOperator());
        assertEquals(Types.DQThresholdViolationLevel.ERROR, testSet.getExpectation().getSeverity());
        assertNotNull(testSet.getExpectation().getThreshold());
        assertTrue(testSet.getExpectation().getThreshold().isAbsolute());
        assertFalse(testSet.getExpectation().getThreshold().isRelative());
        assertEquals(0, testSet.getExpectation().getThreshold().getValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    void readsRelativeThreshold() throws Exception {
        QualityMetricsConfTestSetDTO testSet = read("""
                {
                  "type": "sparkSQL",
                  "scripts": [{"key": "dq.non_zero_count.sql"}],
                  "expectation": {
                    "valueColumn": "value",
                    "operator": ">=",
                    "threshold": {
                      "relativeTo": "non_zero_count",
                      "factor": 0.5
                    },
                    "severity": "warn"
                  }
                }
                """);

        QualityMetricsThresholdDTO threshold = testSet.getExpectation().getThreshold();
        assertNotNull(threshold);
        assertTrue(threshold.isRelative());
        assertFalse(threshold.isAbsolute());
        assertEquals("non_zero_count", threshold.getRelativeTo());
        assertEquals(0, threshold.getFactor().compareTo(new BigDecimal("0.5")));
        assertEquals(Types.DQThresholdViolationLevel.WARN, testSet.getExpectation().getSeverity());
    }

    @Test
    void readsFractionalAbsoluteThresholdAndStringNumber() throws Exception {
        QualityMetricsConfTestSetDTO fractional = read("""
                {
                  "type": "sparkSQL",
                  "expectation": {
                    "operator": "<",
                    "threshold": 0.25
                  }
                }
                """);
        assertEquals(0, fractional.getExpectation().getThreshold().getValue()
                .compareTo(new BigDecimal("0.25")));
        // valueColumn has a default so legacy configs may omit it
        assertEquals("value", fractional.getExpectation().getValueColumn());
        assertNull(fractional.getExpectation().getSeverity());

        QualityMetricsConfTestSetDTO stringNumber = read("""
                {
                  "type": "sparkSQL",
                  "expectation": {
                    "operator": ">",
                    "threshold": "10"
                  }
                }
                """);
        assertEquals(0, stringNumber.getExpectation().getThreshold().getValue()
                .compareTo(BigDecimal.TEN));
    }

    @Test
    void severityIsCaseInsensitive() throws Exception {
        QualityMetricsConfTestSetDTO upper = read("""
                {
                  "type": "sparkSQL",
                  "expectation": {"operator": "=", "threshold": 0, "severity": "WARN"}
                }
                """);
        assertEquals(Types.DQThresholdViolationLevel.WARN, upper.getExpectation().getSeverity());

        QualityMetricsConfTestSetDTO lower = read("""
                {
                  "type": "sparkSQL",
                  "expectation": {"operator": "=", "threshold": 0, "severity": "warn"}
                }
                """);
        assertEquals(Types.DQThresholdViolationLevel.WARN, lower.getExpectation().getSeverity());
    }

    @Test
    void keepsLegacyBehaviourWhenExpectationIsAbsent() throws Exception {
        QualityMetricsConfTestSetDTO testSet = read("""
                {
                  "description": "legacy threshold",
                  "type": "sparkSQL",
                  "scripts": [{"key": "dq.non_zero_count_th.sql"}]
                }
                """);

        assertNull(testSet.getExpectation());
        assertEquals("legacy threshold", testSet.getDescription());
        assertEquals(Types.DQMetricTestSetType.SPARK_SQL, testSet.getType());
        assertEquals(1, testSet.getScripts().size());
        assertEquals("dq.non_zero_count_th.sql", testSet.getScripts().get(0).getKey());
    }

    @Test
    void serializationRoundTripKeepsExpectation() throws Exception {
        String json = """
                {
                  "type": "sparkSQL",
                  "scripts": [{"key": "dq.non_zero_count.sql"}],
                  "expectation": {
                    "valueColumn": "value",
                    "operator": ">=",
                    "threshold": {"relativeTo": "non_zero_count", "factor": 0.5},
                    "severity": "warn"
                  }
                }
                """;

        QualityMetricsConfTestSetDTO original = read(json);
        QualityMetricsConfTestSetDTO restored = read(ObjectMapping.asJsonStringPretty(original));

        assertEquals(original, restored);
    }
}
