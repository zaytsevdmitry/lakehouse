package lakehouse.api.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetSourceDTO that = (DataSetSourceDTO) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProperties());
    }
}
