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
