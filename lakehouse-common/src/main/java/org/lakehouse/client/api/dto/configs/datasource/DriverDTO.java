package org.lakehouse.client.api.dto.configs.datasource;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverDTO {
    private String keyName;
    private String description;
    private Map<Types.ConnectionType,String> connectionTemplates = new HashMap<>();
    private SQLTemplateDTO sqlTemplate;
    private Types.DataSourceType dataSourceType;
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Map<Types.ConnectionType, String> getConnectionTemplates() {
        return connectionTemplates;
    }

    public void setConnectionTemplates(Map<Types.ConnectionType, String> connectionTemplates) {
        this.connectionTemplates = connectionTemplates;
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

    public Types.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Types.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DriverDTO driverDTO = (DriverDTO) o;
        return Objects.equals(getKeyName(), driverDTO.getKeyName()) && Objects.equals(getDescription(), driverDTO.getDescription()) && Objects.equals(getConnectionTemplates(), driverDTO.getConnectionTemplates()) && Objects.equals(getSqlTemplate(), driverDTO.getSqlTemplate()) && getDataSourceType() == driverDTO.getDataSourceType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getDescription(), getConnectionTemplates(), getSqlTemplate(), getDataSourceType());
    }

    @Override
    public String toString() {
        return "DriverDTO{" + "keyName='" + keyName + '\'' + ", description='" + description + '\'' + ", connectionTemplates=" + connectionTemplates + ", sqlTemplate=" + sqlTemplate + ", dataSourceType=" + dataSourceType + '}';
    }
}
