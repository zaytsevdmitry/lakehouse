package org.lakehouse.client.api.dto.configs.datasource;

import org.lakehouse.client.api.constant.Types;

import java.util.*;

public class DataSourceDTO {
    private String keyName;
    private Types.DataSourceType dataSourceType;
    private Types.DataSourceServiceType dataSourceServiceType;
    private List<ServiceDTO> services = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private String description;


    public DataSourceDTO() {
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
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

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public Types.DataSourceServiceType getDataSourceServiceType() {
        return dataSourceServiceType;
    }

    public void setDataSourceServiceType(Types.DataSourceServiceType dataSourceServiceType) {
        this.dataSourceServiceType = dataSourceServiceType;
    }

    public List<ServiceDTO> getServices() {
        return services;
    }

    public void setServices(List<ServiceDTO> services) {
        this.services = services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceDTO that = (DataSourceDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName()) && getDataSourceType() == that.getDataSourceType() && getDataSourceServiceType() == that.getDataSourceServiceType() && Objects.equals(getServices(), that.getServices()) && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getDataSourceType(), getDataSourceServiceType(), getServices(), getProperties(), getDescription());
    }
}
