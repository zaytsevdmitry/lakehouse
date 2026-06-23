/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.client.api.utils.conf;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfUtil {

    final private static Logger logger = LoggerFactory.getLogger(ConfUtil.class);

    /**
     * <p>Filters out keys that begin with a prefix.</>
     * * @param conf - source map
     * @param startWith - prefix
     * @return result - rest of map
     ** @since 0.3.0
     * */
    static Map<String,String> startWith(Map<String, String> map, String startWith){
        return map
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith(startWith))
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));
    }

    /**
     * <p>Filters out keys that begin with a prefix, strips off the prefix</>
     * @param conf - source map
     * @param startWith - prefix
     * @return result - rest of map, with rewrote keys
     * @since 0.5.0
     * */
    public static Map<String, String> extractConf(Map<String, String> conf, String startWith) {
        if (conf == null || startWith == null) {
            logger.warn("{}.extractConf Map or 'startWith' is empty", ConfUtil.class.getName());
            return new HashMap<>();
        }
        return conf.entrySet()
                .stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith(startWith))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(startWith.length()),
                        e -> e.getValue() != null ? e.getValue() : ""
                ));
    }

    /**
     * Safely retrieves a Long value by key from the configuration map.
     *
     * @param map - configuration map
     * @param key - property key
     * @param defaultValue - fallback value if key is not found
     * @return Long value
     * @throws TaskConfigurationException if parsing fails or key is missing without a default value
     * @since 0.5.0
     */
    public static Long getLongByKey(Map<String, String> map, String key, Long defaultValue) throws TaskConfigurationException {
        if (map == null || key == null) {
            logger.warn("{}.getLongByKey Map or key is empty", ConfUtil.class.getName());
            return getLongDefaultOrThrow(key, defaultValue);
        }

        String value = map.get(key);

        if (value != null && !value.isBlank()) {
            try {
                return Long.valueOf(value.trim());
            } catch (NumberFormatException e) {
                throw new TaskConfigurationException(e);
            }
        }

        return getLongDefaultOrThrow(key, defaultValue);
    }

    private static Long getLongDefaultOrThrow(String key, Long defaultValue) throws TaskConfigurationException {
        if (defaultValue == null) {
            throw new TaskConfigurationException(String.format("Key not found: %s", key));
        }
        return defaultValue;
    }

    /**
     * Safely retrieves a boolean value by key from the configuration map.
     *
     * @param map - configuration map
     * @param key - property key
     * @param defaultValue - fallback value if key is not found
     * @return boolean value
     * @throws TaskConfigurationException if the value is not a valid boolean representation
     * @since 0.5.0
     */
    public static boolean getBooleanByKey(Map<String, String> map, String key, boolean defaultValue) throws TaskConfigurationException {
        if (map == null || key == null) {
            logger.warn("{}.getBooleanByKey Map or key is empty", ConfUtil.class.getName());
            return defaultValue;
        }

        String value = map.get(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String trimmedValue = value.trim();
        if ("true".equalsIgnoreCase(trimmedValue)) {
            return true;
        } else if ("false".equalsIgnoreCase(trimmedValue)) {
            return false;
        } else {
            throw new TaskConfigurationException(
                    String.format(
                            "Wrong value %s for key %s when expected boolean { true or false in any case }",
                            value, key));
        }
    }

    /**
     * Convert to string map from another generic map.
     *
     * @param map - other generic types map
     * @return map of strings
     * @since 0.5.0
     */
    public static <K,V> Map<String, String> castToStringMap(Map<K,V> map){
        return map
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey() != null ? e.getKey().toString() : "null",
                        e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
    }
}
