package org.lakehouse.client.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkConfUtil {
    private static Map<String,String> startWith(Map<String,String> map, String prefix){
        return map
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public static Map<String,String> startWithSpark(Map<String,String> map){
        return startWith(map,"spark.");
    }

    public static Map<String,String> startWithSparkCatalog(Map<String,String> map){
        return startWith(map,"spark.catalog.");
    }


    public static Map<String, String> extractAppConf(Map<String, String> props) {
        return props.entrySet()
                .stream()
                .filter(sse -> !sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
