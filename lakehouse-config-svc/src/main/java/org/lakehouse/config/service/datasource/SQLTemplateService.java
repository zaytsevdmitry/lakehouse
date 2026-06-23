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

package org.lakehouse.config.service.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.SQLTemplate;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.exception.ConfigCorruptException;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.SQLTemplateRepository;
import org.lakehouse.config.specifier.SQLTemplateEntitySpecifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SQLTemplateService {
    private final SQLTemplateRepository sqlTemplateRepository;

    public SQLTemplateService(SQLTemplateRepository sqlTemplateRepository) {
        this.sqlTemplateRepository = sqlTemplateRepository;
    }

    public void save(Driver driver, SQLTemplateDTO sqlTemplateDTO)  {
        save(sqlTemplateRepository
                        .findByDriverKeyName(driver.getKeyName()),
                sqlTemplateDTO
                ,driver,null,null
        );
    }
    public void save(DataSource dataSource,SQLTemplateDTO sqlTemplateDTO) {
        save(sqlTemplateRepository
                .findByDataSourceKeyName(dataSource.getKeyName()),
                sqlTemplateDTO,
                null,dataSource,null);
    }
    public void save(DataSet dataSet,SQLTemplateDTO sqlTemplateDTO) {
        save(sqlTemplateRepository
                        .findByDataSourceKeyName(dataSet.getKeyName()),
                sqlTemplateDTO,
                null,null,dataSet);
    }
    private void save(
            List<SQLTemplate> sqlTemplates,
            SQLTemplateDTO sqlTemplateDTO,
            Driver driver,
            DataSource dataSource,
            DataSet dataSet) {
        try {
            new KeyValueEntityMerger(
                    new SQLTemplateEntitySpecifier(sqlTemplateRepository,driver,dataSource,dataSet))
                    .mergeAbstractKeyValues(
                            sqlTemplates
                                    .stream()
                                    .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                                    .toList(),
                            ObjectMapping
                                    .asMapOfStrings(sqlTemplateDTO)
                                    .entrySet()
                                    .stream()
                                    .filter(s->s.getValue()!=null)
                                    .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))
                    );
        } catch (JsonProcessingException e) {
            throw new ConfigCorruptException(e);
        }
    }
    private SQLTemplateDTO mapSQSqlTemplateToDTO(List<SQLTemplate> sqlTemplates) {
        try {
            return ObjectMapping.stringToObject(
                    ObjectMapping.asJsonStringPretty(
                            sqlTemplates
                                    .stream()
                                    .map(sqlTemplate -> Map.entry(sqlTemplate.getKey(),sqlTemplate.getValue()))
                                    .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))),
                    SQLTemplateDTO.class);
        } catch (IOException e) {
            throw new ConfigCorruptException(e);
        }

    }
    public SQLTemplateDTO getSqlTemplateDTO(Driver driver)  {
        return mapSQSqlTemplateToDTO(sqlTemplateRepository.findByDriverKeyName(driver.getKeyName()));
    }
    public SQLTemplateDTO getSqlTemplateDTO(DataSource dataSource)  {
        return mapSQSqlTemplateToDTO(sqlTemplateRepository.findByDataSourceKeyName(dataSource.getKeyName()));
    }
    public SQLTemplateDTO getSqlTemplateDTO(DataSet dataSet)  {
        return mapSQSqlTemplateToDTO(sqlTemplateRepository.findByDataSetKeyName(dataSet.getKeyName()));
    }

}
