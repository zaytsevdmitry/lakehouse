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

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.ui.dto.CatalogNodeDTO;
import org.lakehouse.ui.dto.DataSetNodeDTO;
import org.lakehouse.ui.dto.DataSourceNodeDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    @Test
    void buildsCatalogTreeGroupingDataSourcesByCatalogAndDataSetsByDataSource() {
        ConfigRestClientApi api = mock(ConfigRestClientApi.class);

        DataSourceDTO lakehouseStorage = dataSource("lakehousestorage", "127.0.0.1", "5432", "lakehouse");
        DataSourceDTO demo = dataSource("demo", "127.0.0.1", "5432", "demo");
        when(api.getDataSourceDTOList()).thenReturn(List.of(demo, lakehouseStorage));

        DataSetDTO transaction = dataSet("transaction_processing", "demo", "public", "transaction_t");
        when(api.getDataSetDTOList()).thenReturn(List.of(transaction));

        List<CatalogNodeDTO> tree = new CatalogService(api).getCatalogTree();

        assertThat(tree).hasSize(2);
        CatalogNodeDTO demoCatalog = tree.get(0);
        assertThat(demoCatalog.getCatalogKeyName()).isEqualTo("demo");
        assertThat(demoCatalog.getDataSources()).hasSize(1);
        DataSourceNodeDTO demoDs = demoCatalog.getDataSources().get(0);
        assertThat(demoDs.getKeyName()).isEqualTo("demo");
        assertThat(demoDs.getService().getHost()).isEqualTo("127.0.0.1");
        assertThat(demoDs.getService().getPort()).isEqualTo("5432");
        assertThat(demoDs.getDataSets()).hasSize(1);
        DataSetNodeDTO dataset = demoDs.getDataSets().get(0);
        assertThat(dataset.getKeyName()).isEqualTo("transaction_processing");
        assertThat(dataset.getTableName()).isEqualTo("transaction_t");

        CatalogNodeDTO lakehouseCatalog = tree.get(1);
        assertThat(lakehouseCatalog.getCatalogKeyName()).isEqualTo("lakehousestorage");
        DataSourceNodeDTO lakehouseDs = lakehouseCatalog.getDataSources().get(0);
        assertThat(lakehouseDs.getKeyName()).isEqualTo("lakehousestorage");
        assertThat(lakehouseDs.getDataSets()).isEmpty();
    }

    private DataSourceDTO dataSource(String keyName, String host, String port, String urn) {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setKeyName(keyName);
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setHost(host);
        serviceDTO.setPort(port);
        serviceDTO.setUrn(urn);
        dto.setService(serviceDTO);
        return dto;
    }

    private DataSetDTO dataSet(String keyName, String dataSourceKeyName, String schema, String table) {
        DataSetDTO dto = new DataSetDTO();
        dto.setKeyName(keyName);
        dto.setDataSourceKeyName(dataSourceKeyName);
        dto.setDatabaseSchemaName(schema);
        dto.setTableName(table);
        return dto;
    }
}
