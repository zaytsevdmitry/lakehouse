package org.lakehouse.client.api.factory;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.utils.ObjectMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLTemplateFactory {
    private static Map<String,String> cleanMap(Map<String,String> map){
        return map.entrySet()
                .stream()
                .filter(e -> e.getValue() !=null)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public static SQLTemplateDTO mergeSqlTemplate(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO) throws IOException {
        Map<String,String> resultMap = new HashMap<>();
        resultMap.putAll(ObjectMapping.asMapOfStrings(driverDTO.getSqlTemplate()));
        resultMap.putAll(cleanMap(ObjectMapping.asMapOfStrings(dataSourceDTO.getSqlTemplate())));
        resultMap.putAll(cleanMap(ObjectMapping.asMapOfStrings(dataSetDTO.getSqlTemplate())));
        return ObjectMapping
                .stringToObject(
                        ObjectMapping.asJsonString(resultMap),
                        SQLTemplateDTO.class);
    }
}
