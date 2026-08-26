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

package org.lakehouse.client.api.utils.conf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.exception.TaskConfigurationException;

import java.util.HashMap;
import java.util.Map;

public class ConfUtilTest {

    // ==========================================
    // Tests for getBooleanByKey()
    // ==========================================

    @Test
    public void shouldReturnCorrectBooleanValues() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("key.true", "true");
        map.put("key.false", "false");
        map.put("key.upper", "TRUE");

        Assertions.assertTrue(ConfUtil.getBooleanByKey(map, "key.true", false));
        Assertions.assertFalse(ConfUtil.getBooleanByKey(map, "key.false", true));
        Assertions.assertTrue(ConfUtil.getBooleanByKey(map, "key.upper", false));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyMissingOrBlank() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("key.blank", "   ");
        map.put("key.null", null);

        // Key is completely missing
        Assertions.assertTrue(ConfUtil.getBooleanByKey(map, "missing.key", true));
        Assertions.assertFalse(ConfUtil.getBooleanByKey(map, "missing.key", false));

        // Key contains only whitespaces or is null
        Assertions.assertTrue(ConfUtil.getBooleanByKey(map, "key.blank", true));
        Assertions.assertFalse(ConfUtil.getBooleanByKey(map, "key.null", false));
    }

    @Test
    public void shouldThrowExceptionOnInvalidBooleanString() {
        Map<String, String> map = new HashMap<>();
        map.put("key.invalid", "not-a-boolean");

        TaskConfigurationException exception = Assertions.assertThrows(
                TaskConfigurationException.class,
                () -> ConfUtil.getBooleanByKey(map, "key.invalid", true)
        );
        Assertions.assertTrue(exception.getMessage().contains("key.invalid"));
    }

    // ==========================================
    // Tests for getLongByKey()
    // ==========================================

    @Test
    public void shouldReturnCorrectLongValue() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("timeout", "5000");

        Assertions.assertEquals(Long.valueOf(5000L), ConfUtil.getLongByKey(map, "timeout", 1000L));
    }

    @Test
    public void shouldReturnDefaultValueWhenMissingOrEmpty() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("empty.key", "");
        map.put("null.value", null);

        Assertions.assertEquals(Long.valueOf(100L), ConfUtil.getLongByKey(map, "missing.key", 100L));
        Assertions.assertEquals(Long.valueOf(200L), ConfUtil.getLongByKey(map, "empty.key", 200L));
        Assertions.assertEquals(Long.valueOf(300L), ConfUtil.getLongByKey(map, "null.value", 300L));
    }

    @Test
    public void shouldThrowExceptionWhenDefaultValueIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put("empty.key", "");

        Assertions.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "missing.key", null));
        Assertions.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "empty.key", null));
    }

    @Test
    public void shouldThrowExceptionOnInvalidLongFormat() {
        Map<String, String> map = new HashMap<>();
        map.put("timeout", "abc");

        Assertions.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "timeout", 1000L));
    }

    // ==========================================
    // Tests for extractConf()
    // ==========================================

    @Test
    public void shouldFilterAndStripPrefix() {
        Map<String, String> map = new HashMap<>();
        map.put("spark.driver.memory", "4g");
        map.put("spark.executor.memory", "2g");
        map.put("hadoop.fs.defaultFS", "hdfs://localhost");

        Map<String, String> result = ConfUtil.extractConf(map, "spark.");

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("4g", result.get("driver.memory"));
        Assertions.assertEquals("2g", result.get("executor.memory"));
        Assertions.assertFalse(result.containsKey("hadoop.fs.defaultFS"));
    }

    @Test
    public void shouldHandleNullInputSafely() {
        Assertions.assertTrue(ConfUtil.extractConf(null, "prefix.").isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Assertions.assertTrue(ConfUtil.extractConf(map, null).isEmpty());
    }

    // ==========================================
    // Tests for castToStringMap()
    // ==========================================

    @Test
    public void shouldCastTypesToStrings() {
        Map<Object, Object> complexMap = new HashMap<>();
        complexMap.put(123, 456L);
        complexMap.put("status", true);
        complexMap.put(null, "value");
        complexMap.put("empty", null);

        Map<String, String> stringMap = ConfUtil.castToStringMap(complexMap);

        Assertions.assertEquals("456", stringMap.get("123"));
        Assertions.assertEquals("true", stringMap.get("status"));
        Assertions.assertEquals("value", stringMap.get("null")); // Key null becomes string "null"
        Assertions.assertEquals("", stringMap.get("empty"));     // Value null becomes empty string
    }
}
