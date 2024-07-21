package lakehouse.api.dto;

import java.io.Serializable;
import java.util.Map;

public class DataSetSourceDTO implements Serializable {
    private String name;
    private Map<String,String> properties;

    public DataSetSourceDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
