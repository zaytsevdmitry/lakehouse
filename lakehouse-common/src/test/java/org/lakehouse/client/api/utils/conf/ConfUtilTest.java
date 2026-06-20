package org.lakehouse.client.api.utils.conf;

import org.junit.Assert;
import org.junit.Test;
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

        Assert.assertTrue(ConfUtil.getBooleanByKey(map, "key.true", false));
        Assert.assertFalse(ConfUtil.getBooleanByKey(map, "key.false", true));
        Assert.assertTrue(ConfUtil.getBooleanByKey(map, "key.upper", false));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyMissingOrBlank() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("key.blank", "   ");
        map.put("key.null", null);

        // Key is completely missing
        Assert.assertTrue(ConfUtil.getBooleanByKey(map, "missing.key", true));
        Assert.assertFalse(ConfUtil.getBooleanByKey(map, "missing.key", false));

        // Key contains only whitespaces or is null
        Assert.assertTrue(ConfUtil.getBooleanByKey(map, "key.blank", true));
        Assert.assertFalse(ConfUtil.getBooleanByKey(map, "key.null", false));
    }

    @Test
    public void shouldThrowExceptionOnInvalidBooleanString() {
        Map<String, String> map = new HashMap<>();
        map.put("key.invalid", "not-a-boolean");

        TaskConfigurationException exception = Assert.assertThrows(
                TaskConfigurationException.class,
                () -> ConfUtil.getBooleanByKey(map, "key.invalid", true)
        );
        Assert.assertTrue(exception.getMessage().contains("key.invalid"));
    }

    // ==========================================
    // Tests for getLongByKey()
    // ==========================================

    @Test
    public void shouldReturnCorrectLongValue() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("timeout", "5000");

        Assert.assertEquals(Long.valueOf(5000L), ConfUtil.getLongByKey(map, "timeout", 1000L));
    }

    @Test
    public void shouldReturnDefaultValueWhenMissingOrEmpty() throws TaskConfigurationException {
        Map<String, String> map = new HashMap<>();
        map.put("empty.key", "");
        map.put("null.value", null);

        Assert.assertEquals(Long.valueOf(100L), ConfUtil.getLongByKey(map, "missing.key", 100L));
        Assert.assertEquals(Long.valueOf(200L), ConfUtil.getLongByKey(map, "empty.key", 200L));
        Assert.assertEquals(Long.valueOf(300L), ConfUtil.getLongByKey(map, "null.value", 300L));
    }

    @Test
    public void shouldThrowExceptionWhenDefaultValueIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put("empty.key", "");

        Assert.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "missing.key", null));
        Assert.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "empty.key", null));
    }

    @Test
    public void shouldThrowExceptionOnInvalidLongFormat() {
        Map<String, String> map = new HashMap<>();
        map.put("timeout", "abc");

        Assert.assertThrows(TaskConfigurationException.class, () -> ConfUtil.getLongByKey(map, "timeout", 1000L));
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

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("4g", result.get("driver.memory"));
        Assert.assertEquals("2g", result.get("executor.memory"));
        Assert.assertFalse(result.containsKey("hadoop.fs.defaultFS"));
    }

    @Test
    public void shouldHandleNullInputSafely() {
        Assert.assertTrue(ConfUtil.extractConf(null, "prefix.").isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Assert.assertTrue(ConfUtil.extractConf(map, null).isEmpty());
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

        Assert.assertEquals("456", stringMap.get("123"));
        Assert.assertEquals("true", stringMap.get("status"));
        Assert.assertEquals("value", stringMap.get("null")); // Key null becomes string "null"
        Assert.assertEquals("", stringMap.get("empty"));     // Value null becomes empty string
    }
}
