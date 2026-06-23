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

package org.lakehouse.client.api.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SourceConfDTO {
    private String targetDataSetKeyName;
    private Map<String, DriverDTO> drivers = new HashMap<>();
    private Map<String, DataSourceDTO> dataSources = new HashMap<>();
    private Map<String, DataSetDTO> dataSets = new HashMap<>();

    public String getTargetDataSetKeyName() {
        return targetDataSetKeyName;
    }

    public void setTargetDataSetKeyName(String targetDataSetKeyName) {
        this.targetDataSetKeyName = targetDataSetKeyName;
    }

    public Map<String, DriverDTO> getDrivers() {
        return drivers;
    }

    public void setDrivers(Map<String, DriverDTO> drivers) {
        this.drivers = drivers;
    }

    public Map<String, DataSourceDTO> getDataSources() {
        return dataSources;
    }

    public void setDataSources(Map<String, DataSourceDTO> dataSources) {
        this.dataSources = dataSources;
    }

    public Map<String, DataSetDTO> getDataSets() {
        return dataSets;
    }

    public void setDataSets(Map<String, DataSetDTO> dataSets) {
        this.dataSets = dataSets;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SourceConfDTO that = (SourceConfDTO) o;
        return Objects.equals(targetDataSetKeyName, that.targetDataSetKeyName) && Objects.equals(drivers, that.drivers) && Objects.equals(dataSources, that.dataSources) && Objects.equals(dataSets, that.dataSets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetDataSetKeyName, drivers, dataSources, dataSets);
    }

    @Override
    public String toString() {
        return "SourceConfDTO{" +
                "targetDataSetKeyName='" + targetDataSetKeyName + '\'' +
                ", drivers=" + drivers +
                ", dataSources=" + dataSources +
                ", dataSets=" + dataSets +
                '}';
    }

    @JsonIgnore
    public DataSourceDTO getDataSourceDTOByDataSetKeyName(String dataSetKeyName){
        return getDataSources().get( getDataSets().get(dataSetKeyName).getDataSourceKeyName());
    }

    @JsonIgnore
    public DriverDTO getDriverDTOByDataSetKeyName(String dataSetKeyName){
        return getDrivers()
                .get(getDataSources()
                        .get(getDataSets()
                                .get(dataSetKeyName).getDataSourceKeyName())
                        .getDriverKeyName());
    }

    @JsonIgnore
    public DataSetDTO getTargetDataSet(){
        return dataSets.get(getTargetDataSetKeyName());
    }

    @JsonIgnore
    public DataSourceDTO getTargetDataSource(){
        return getDataSources().get( getTargetDataSet().getDataSourceKeyName());
    }
    @JsonIgnore
    public DriverDTO getTargetDriver(){
        return getDrivers()
                .get(getTargetDataSource()
                        .getDriverKeyName());
    }
}
