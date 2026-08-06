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
package org.lakehouse.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class DataSourceNodeDTO {

    private String keyName;
    private String description;
    private String driverKeyName;
    private ServiceInfoDTO service;
    private List<DataSetNodeDTO> dataSets = new ArrayList<>();

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

    public String getDriverKeyName() {
        return driverKeyName;
    }

    public void setDriverKeyName(String driverKeyName) {
        this.driverKeyName = driverKeyName;
    }

    public ServiceInfoDTO getService() {
        return service;
    }

    public void setService(ServiceInfoDTO service) {
        this.service = service;
    }

    public List<DataSetNodeDTO> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<DataSetNodeDTO> dataSets) {
        this.dataSets = dataSets;
    }
}
