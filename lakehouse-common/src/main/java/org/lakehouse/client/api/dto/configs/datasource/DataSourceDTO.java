package org.lakehouse.client.api.dto.configs.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.*;

public class DataSourceDTO {
    private String keyName;
    //private List<ServiceDTO> services = new ArrayList<>();
    ServiceDTO service;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public ServiceDTO getService() {
        return service;
    }

    public void setService(ServiceDTO service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "DataSourceDTO{" +
                "keyName='" + keyName + '\'' +
                ", service=" + service +
                ", description='" + description + '\'' +
                ", sqlTemplate=" + sqlTemplate +
                ", driverKeyName='" + driverKeyName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceDTO that = (DataSourceDTO) o;
        return Objects.equals(keyName, that.keyName) && Objects.equals(service, that.service) && Objects.equals(description, that.description) && Objects.equals(sqlTemplate, that.sqlTemplate) && Objects.equals(driverKeyName, that.driverKeyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, service, description, sqlTemplate, driverKeyName);
    }

}
