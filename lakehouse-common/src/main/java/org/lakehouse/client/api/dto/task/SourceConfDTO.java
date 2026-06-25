/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
