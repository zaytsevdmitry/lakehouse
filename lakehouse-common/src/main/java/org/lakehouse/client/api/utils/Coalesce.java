package org.lakehouse.client.api.utils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
     * @since 0.4.0
     *  */
    public static Map<String, String> applyMergeNonNullValuesMap(Map<String, String> currentMap, Map<String, String> newMap) {
        Map<String, String> result = new HashMap<>();
        result.putAll(currentMap);
        result.putAll(newMap.entrySet()
                .stream()
                .filter(e -> e.getValue() !=null)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue)));
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
