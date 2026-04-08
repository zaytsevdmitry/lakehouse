package org.lakehouse.common;

import java.util.Map;

public interface ConfigurationProperties {
    Map<String, String> getProperties();
    void setProperties(Map<String, String> properties);
}
