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
