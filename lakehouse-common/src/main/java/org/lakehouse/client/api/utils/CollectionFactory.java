package org.lakehouse.client.api.utils;


import java.util.Map;
import java.util.Properties;

public class CollectionFactory {
    public static Properties mapToProperties(Map<String, String> options) {
        Properties properties = new Properties();
        properties.putAll(options);
        return properties;
    }
}
