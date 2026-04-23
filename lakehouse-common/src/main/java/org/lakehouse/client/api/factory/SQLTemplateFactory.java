package org.lakehouse.client.api.factory;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLTemplateFactory {
    final private static Logger logger = LoggerFactory.getLogger(SQLTemplateFactory.class);


        private static Map<String,String> nonNullValuesMap(Map<String,String> map){
            return map.entrySet()
                    .stream()
                    .filter(e -> e.getValue() !=null)
                    .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
        }
    public static SQLTemplateDTO mergeSqlTemplate(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO) throws TaskConfigurationException {
        try {



            Map<String, String> resultMap =
                    Coalesce.applyMergeNonNullValuesMap(
                            Coalesce.applyMergeNonNullValuesMap(
                                    ObjectMapping.asMapOfStrings(driverDTO.getSqlTemplate()),
                                    ObjectMapping.asMapOfStrings(dataSourceDTO.getSqlTemplate())
                            ),
                            ObjectMapping.asMapOfStrings(dataSetDTO.getSqlTemplate()));

            List<String> nullableKeys = resultMap
                    .entrySet()
                    .stream()
                    .filter(s -> s.getValue() == null)
                    .map(Map.Entry::getKey)
                    .toList();

            if (!nullableKeys.isEmpty()) {

                logger.error(
                        "{} keys with null value {}",
                        SQLTemplateDTO.class.getSimpleName(),
                         nullableKeys.stream().map(s-> String.format("\"%s\": null,", s)).collect(Collectors.joining("\n")));
                throw new TaskConfigurationException(
                        "Configuration of " + SQLTemplateDTO.class.getSimpleName() + "  contains fields with null values. " +
                                "Check templates in driver: " + driverDTO.getKeyName() +
                                " dataSource: " + dataSourceDTO.getKeyName() +
                                " dataSet: " + dataSetDTO.getKeyName()
                );
            }

            return ObjectMapping
                    .stringToObject(
                            ObjectMapping.asJsonString(resultMap),
                            SQLTemplateDTO.class);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }
}
