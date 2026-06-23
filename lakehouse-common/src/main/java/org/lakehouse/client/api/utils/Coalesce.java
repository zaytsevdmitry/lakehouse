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

package org.lakehouse.client.api.utils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class Coalesce {
    public static String apply(String s1, String s2) {
        if (s1 == null || s1.trim().isBlank()) return s2;
        else return s1;
    }
    public static String apply(String s1, String s2, String s3) {
        String string1 = apply(s1,s2);
        if (string1 == null || string1.trim().isBlank()) return s3;
        else return string1;
    }


    /**
     * <p>
     * override  map if new value not null
     * </p>
     * @param currentMap for override
     * @param newMap overriding map
     * @return map with override values if new values is not null
     * @since 0.3.0
     *  */
    public static  <K, V>  Map <K, V>  applyMergeNonNullValuesMap(Map <K, V>  currentMap, Map <K, V>  newMap) {
        Map<K, V> result = new HashMap<>();
        if (currentMap != null) {
            result.putAll(currentMap);
        }
        if (newMap != null) {
            newMap.forEach((key, value) -> {
                if (value != null) {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

    /**
     * <p>
     * override  map by priority
     * </p>
     *
     * @param ms1 the priority map of strings
     * @param ms2 the secondary map of strings
     * @return merged map
     * @since 0.0.1
     */
    public static Map<String, String> applyRewriteStringMap(Map<String, String> ms1, Map<String, String> ms2) {
        Map<String, String> result = new HashMap<>();
        result.putAll(ms2);
        result.putAll(ms1); // rewrite and append
        return result;
    }

    public static OffsetDateTime apply(OffsetDateTime odt1, OffsetDateTime odt2) {
        if (odt1 == null) return odt2;
        else return odt1;
    }
}
