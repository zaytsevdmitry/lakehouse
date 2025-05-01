package org.lakehouse.client.api.utils;

import java.util.HashMap;
import java.util.Map;

public class Coalesce {
    public static String apply(String s1, String s2){
        if (s1 == null || s1.trim().isBlank()) return s2;
        else return s1;
    }

    /**
     * <p>
     *     merge two map by priority
     * </p>
     * @param ms1 the priority map of strings
     * @param ms2 the secondary map of strings
     * @return the priority
     * @since 0.0.1
     */
    public static Map<String,String> applyStringMap(Map<String,String> ms1, Map<String,String> ms2){
        Map<String,String> result = new HashMap<>();
        result.putAll(ms2);
        result.putAll(ms1); // rewrite and append
        return result;
    }

}
