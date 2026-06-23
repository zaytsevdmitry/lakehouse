/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
