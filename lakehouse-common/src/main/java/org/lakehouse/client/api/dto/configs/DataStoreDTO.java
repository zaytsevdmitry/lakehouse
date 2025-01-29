package org.lakehouse.client.api.dto.configs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataStoreDTO {
    private String name;
    private String interfaceType;
    private String vendor;
    private Map<String,String> properties = new HashMap<>();
    private String description;

    public DataStoreDTO(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataStoreDTO that = (DataStoreDTO) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getInterfaceType(), that.getInterfaceType()) && Objects.equals(getVendor(), that.getVendor()) && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getInterfaceType(), getVendor(), getProperties(), getDescription());
    }
}
