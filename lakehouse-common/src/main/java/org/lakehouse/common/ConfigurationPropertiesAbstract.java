package org.lakehouse.common;
import java.util.Map;
public abstract class ConfigurationPropertiesAbstract implements ConfigurationProperties{

    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}

