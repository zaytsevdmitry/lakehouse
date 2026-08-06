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
package org.lakehouse.ui.service;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.ui.dto.CatalogNodeDTO;
import org.lakehouse.ui.dto.DataSetNodeDTO;
import org.lakehouse.ui.dto.DataSourceNodeDTO;
import org.lakehouse.ui.dto.ServiceInfoDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogService {

    private final ConfigRestClientApi configRestClientApi;

    public CatalogService(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public List<CatalogNodeDTO> getCatalogTree() {
        List<DataSourceDTO> dataSources = configRestClientApi.getDataSourceDTOList();
        List<DataSetDTO> dataSets = configRestClientApi.getDataSetDTOList();

        Map<String, DataSourceNodeDTO> dataSourceByKeyName = new LinkedHashMap<>();
        for (DataSourceDTO dataSource : dataSources) {
            dataSourceByKeyName.put(dataSource.getKeyName(), toDataSourceNodeDTO(dataSource));
        }

        for (DataSetDTO dataSet : dataSets) {
            DataSourceNodeDTO owner = dataSourceByKeyName.get(dataSet.getDataSourceKeyName());
            if (owner != null) {
                owner.getDataSets().add(toDataSetNodeDTO(dataSet));
            }
        }

        Map<String, CatalogNodeDTO> catalogByKeyName = new LinkedHashMap<>();
        for (DataSourceNodeDTO node : dataSourceByKeyName.values()) {
            DataSourceDTO dataSource = findDataSource(dataSources, node.getKeyName());
            String catalogKeyName = dataSource != null && dataSource.getKeyName() != null
                    ? dataSource.getKeyName() : "default";
            catalogByKeyName
                    .computeIfAbsent(catalogKeyName, CatalogNodeDTO::new)
                    .getDataSources()
                    .add(node);
        }

        List<CatalogNodeDTO> result = new ArrayList<>(catalogByKeyName.values());
        result.sort(Comparator.comparing(CatalogNodeDTO::getCatalogKeyName, Comparator.nullsLast(String::compareTo)));
        result.forEach(CatalogService::sortNode);
        return result;
    }

    private DataSourceNodeDTO toDataSourceNodeDTO(DataSourceDTO dataSource) {
        DataSourceNodeDTO node = new DataSourceNodeDTO();
        node.setKeyName(dataSource.getKeyName());
        node.setDescription(dataSource.getDescription());
        ServiceDTO service = dataSource.getService();
        if (service != null) {
            node.setService(new ServiceInfoDTO(service.getHost(), service.getPort(), service.getUrn()));
        }
        return node;
    }

    private DataSetNodeDTO toDataSetNodeDTO(DataSetDTO dataSet) {
        DataSetNodeDTO node = new DataSetNodeDTO();
        node.setKeyName(dataSet.getKeyName());
        node.setNameSpaceKeyName(dataSet.getNameSpaceKeyName());
        node.setDatabaseSchemaName(dataSet.getDatabaseSchemaName());
        node.setTableName(dataSet.getTableName());
        node.setDescription(dataSet.getDescription());
        return node;
    }

    private DataSourceDTO findDataSource(List<DataSourceDTO> dataSources, String keyName) {
        return dataSources.stream()
                .filter(ds -> keyName.equals(ds.getKeyName()))
                .findFirst()
                .orElse(null);
    }

    private static void sortNode(CatalogNodeDTO catalog) {
        catalog.getDataSources().sort(Comparator.comparing(DataSourceNodeDTO::getKeyName, Comparator.nullsLast(String::compareTo)));
        catalog.getDataSources().forEach(ds -> ds.getDataSets().sort(
                Comparator.comparing(DataSetNodeDTO::getKeyName, Comparator.nullsLast(String::compareTo))));
    }
}
