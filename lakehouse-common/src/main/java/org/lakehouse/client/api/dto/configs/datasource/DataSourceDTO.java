package org.lakehouse.client.api.dto.configs.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.*;

public class DataSourceDTO {
    private String keyName;
    private List<ServiceDTO> services = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private String description;
    private SQLTemplateDTO sqlTemplate = new SQLTemplateDTO();
    private String driverKeyName;

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

    public List<ServiceDTO> getServices() {
        return services;
    }

    public void setServices(List<ServiceDTO> services) {
        this.services = services;
    }

    public SQLTemplateDTO getSqlTemplate() {
        return sqlTemplate;
    }

    public void setSqlTemplate(SQLTemplateDTO sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public String getDriverKeyName() {
        return driverKeyName;
    }

    public void setDriverKeyName(String driverKeyName) {
        this.driverKeyName = driverKeyName;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceDTO that = (DataSourceDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getServices(), that.getServices()) && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getSqlTemplate(), that.getSqlTemplate()) && Objects.equals(getDriverKeyName(), that.getDriverKeyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getServices(), getProperties(), getDescription(), getSqlTemplate(), getDriverKeyName());
    }

    @Override
    public String toString() {
        return "DataSourceDTO{" + "keyName='" + keyName + '\'' + ", services=" + services + ", properties=" + properties + ", description='" + description + '\'' + ", sqlTemplate=" + sqlTemplate + ", driverKeyName='" + driverKeyName + '\'' + '}';
    }
}
