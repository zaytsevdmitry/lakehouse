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
