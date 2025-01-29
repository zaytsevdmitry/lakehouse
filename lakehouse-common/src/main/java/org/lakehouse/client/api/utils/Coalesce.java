package org.lakehouse.client.api.utils;

import java.util.HashMap;
import java.util.Map;

public class Coalesce {
    public static String apply(String s1, String s2){
        if (s1 == null || s1.trim().isBlank()) return s2;
        else return s1;
    }
    public static Map<String,String> applyStringMap(Map<String,String> ms1, Map<String,String> ms2){
        Map<String,String> result = new HashMap<>();
        result.putAll(ms1);

        ms2.keySet().forEach(k -> {
            if( !result.containsKey(k) ){
                result.put(k, ms2.get(k));
            }
        });
        return result;
    }

}
