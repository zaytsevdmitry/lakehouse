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

package org.lakehouse.jinja.java.util;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.HashMap;
import java.util.Map;

public class SourceConfUtil {
    private final JinJavaUtils jinJavaUtils;

    public SourceConfUtil(JinJavaUtils jinJavaUtils) {
        this.jinJavaUtils = jinJavaUtils;
    }

    public void renderProperties(SourceConfDTO sourceConfDTO){
        Map<String,Object> localContext = new HashMap<>();

        for(DriverDTO driverDTO:sourceConfDTO.getDrivers().values()){
            localContext.put(SystemVarKeys.DRIVER_KEY, driverDTO);
            for (DataSourceDTO dataSourceDTO: sourceConfDTO.getDataSources().values()){
                if(dataSourceDTO.getDriverKeyName().equals(driverDTO.getKeyName())){
                    localContext.put(SystemVarKeys.DATASOURCE_KEY, dataSourceDTO);
                    localContext.put(SystemVarKeys.SERVICE_KEY, dataSourceDTO.getService());
                    dataSourceDTO.getService().setProperties(jinJavaUtils.renderMap(dataSourceDTO.getService().getProperties(),localContext));
                    for (DataSetDTO dataSetDTO:sourceConfDTO.getDataSets().values()){
                        if (dataSetDTO.getDataSourceKeyName().equals(dataSourceDTO.getKeyName())){
                            localContext.put(SystemVarKeys.DATASET_KEY, dataSetDTO);
                            dataSetDTO.setProperties(jinJavaUtils.renderMap(dataSetDTO.getProperties(),localContext));
                        }
                    }
                }
            }
        }
    }
}
