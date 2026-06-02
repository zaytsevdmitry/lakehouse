package org.lakehouse.client.api.utils.conf;

import org.lakehouse.client.api.exception.TaskConfigurationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfUtil {

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
     * * @since 0.4.0
     * */
    public static  Map<String, String> extractConf(Map<String, String> conf, String startWith){
        if (conf == null) {
            return new HashMap<>();
        }
        return conf.entrySet()
                .stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith(startWith))
                // Используем явные лямбда-выражения вместо ссылок на методы ::
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(startWith.length()),
                        e -> e.getValue() != null ? e.getValue() : ""
                ));
    }
    public static Long getLongByKey(Map<String, String> map, String key, Long defaultValue) throws TaskConfigurationException {
        if (map.containsKey(key) || map.get(key).isBlank())
            try {
                return Long.valueOf(map.get(key));
            } catch (NumberFormatException e) {
                throw new TaskConfigurationException(e);
            }
        else {
            if (defaultValue == null)
                throw new TaskConfigurationException(String.format("Key not found %s", key));
            else return defaultValue;
        }
    }

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
