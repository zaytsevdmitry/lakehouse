package org.lakehouse.client.api.dto.configs.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.Objects;

public class DataSourceDTO {
    private String keyName;
    private String catalogKeyName;
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
                ", catalogName='" + catalogKeyName + '\'' +
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
        return Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getCatalogKeyName(), that.getCatalogKeyName()) && Objects.equals(getService(), that.getService()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getSqlTemplate(), that.getSqlTemplate()) && Objects.equals(getDriverKeyName(), that.getDriverKeyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getCatalogKeyName(), getService(), getDescription(), getSqlTemplate(), getDriverKeyName());
    }

    public String getCatalogKeyName() {
        return catalogKeyName;
    }

    public void setCatalogKeyName(String catalogKeyName) {
        this.catalogKeyName = catalogKeyName;
    }
}
