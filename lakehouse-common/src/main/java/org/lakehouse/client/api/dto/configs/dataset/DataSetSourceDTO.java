package org.lakehouse.client.api.dto.configs.dataset;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataSetSourceDTO implements Serializable {
    private static final long serialVersionUID = -2784578257851689101L;

    private String dataSetKeyName;
    private Map<String, String> properties = new HashMap<>();

    public DataSetSourceDTO() {
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
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
        return Objects.equals(getDataSetKeyName(), that.getDataSetKeyName()) && Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getProperties());
    }

    @Override
    public String toString() {
        return "\nDataSetSourceDTO{" +
                "\ndataSetKeyName='" + dataSetKeyName + '\'' +
                "\n, properties=" + properties +
                '}';
    }
}
