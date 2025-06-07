package org.lakehouse.taskexecutor.configuration;


import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConfigurationProperties {

    private  Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}

