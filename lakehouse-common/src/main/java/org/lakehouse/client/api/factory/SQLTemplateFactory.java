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
                            ObjectMapping.asJsonStringPretty(resultMap),
                            SQLTemplateDTO.class);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
    }
}
