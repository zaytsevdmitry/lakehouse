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
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.ui.dto.CatalogTreeNodeDTO;
import org.lakehouse.ui.dto.ConstraintDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    @Test
    void buildsCatalogTreeGroupingByDataSourceSchemaAndTable() {
        ConfigRestClientApi api = mock(ConfigRestClientApi.class);

        DataSetDTO transaction = dataSet("transaction_processing", "demo", "public", "transaction_t");
        DataSetDTO order = dataSet("order_processing", "demo", "public", "order_t");
        DataSetDTO item = dataSet("item_processing", "demo", "internal", "item_t");
        when(api.getDataSetDTOList()).thenReturn(List.of(order, item, transaction));

        List<CatalogTreeNodeDTO> tree = new CatalogService(api).getCatalogTree();

        assertThat(tree).hasSize(1);
        CatalogTreeNodeDTO demoDataSource = tree.get(0);
        assertThat(demoDataSource.getKeyName()).isEqualTo("demo");
        assertThat(demoDataSource.getBadge()).isEqualTo(2);

        CatalogTreeNodeDTO internalSchema = demoDataSource.getChildren().get(0);
        assertThat(internalSchema.getDatabaseSchemaName()).isEqualTo("internal");
        assertThat(internalSchema.getDataSourceKeyName()).isEqualTo("demo");
        assertThat(internalSchema.getBadge()).isEqualTo(1);
        assertThat(internalSchema.getChildren()).hasSize(1);
        assertThat(internalSchema.getChildren().get(0).getTableName()).isEqualTo("item_t");

        CatalogTreeNodeDTO publicSchema = demoDataSource.getChildren().get(1);
        assertThat(publicSchema.getDatabaseSchemaName()).isEqualTo("public");
        assertThat(publicSchema.getBadge()).isEqualTo(2);
        assertThat(publicSchema.getChildren()).extracting(CatalogTreeNodeDTO::getTableName)
                .containsExactly("order_t", "transaction_t");
    }

    @Test
    void buildsEmptyTreeWhenNoDataSetsConfigured() {
        ConfigRestClientApi api = mock(ConfigRestClientApi.class);
        when(api.getDataSetDTOList()).thenReturn(List.of());

        List<CatalogTreeNodeDTO> tree = new CatalogService(api).getCatalogTree();

        assertThat(tree).isEmpty();
    }

    @Test
    void buildsConstraintRowsFlatteningReferenceIntoReferencedTable() {
        ConfigRestClientApi api = mock(ConfigRestClientApi.class);

        DataSetDTO order = dataSet("order_processing", "demo", "public", "order_t");
        DataSetConstraintDTO pk = new DataSetConstraintDTO();
        pk.setType(Types.Constraint.primary);
        pk.setColumns("order_id");
        pk.setConstraintLevelCheck(Types.ConstraintLevelCheck.construct);

        DataSetConstraintDTO fk = new DataSetConstraintDTO();
        fk.setType(Types.Constraint.foreign);
        fk.setColumns("customer_id");
        fk.setConstraintLevelCheck(Types.ConstraintLevelCheck.none);
        ForeignKeyReferenceDTO reference = new ForeignKeyReferenceDTO();
        reference.setDataSetKeyName("customer_processing");
        reference.setConstraintName("pk_customer");
        reference.setOnDelete(Types.ReferenceAction.CASCADE);
        reference.setOnUpdate(Types.ReferenceAction.NO_ACTION);
        fk.setReference(reference);

        Map<String, DataSetConstraintDTO> constraints = new LinkedHashMap<>();
        constraints.put("pk_order", pk);
        constraints.put("fk_order_customer", fk);
        order.setConstraints(constraints);

        DataSetDTO customer = dataSet("customer_processing", "demo", "public", "customer_t");
        when(api.getDataSetDTO("order_processing")).thenReturn(order);
        when(api.getDataSetDTO("customer_processing")).thenReturn(customer);

        List<ConstraintDTO> rows = new CatalogService(api).getConstraints("order_processing");

        assertThat(rows).hasSize(2);
        ConstraintDTO pkRow = rows.get(0);
        assertThat(pkRow.getName()).isEqualTo("pk_order");
        assertThat(pkRow.getType()).isEqualTo("primary");
        assertThat(pkRow.getColumns()).isEqualTo("order_id");
        assertThat(pkRow.isEnabled()).isTrue();
        assertThat(pkRow.getConstraintLevelCheck()).isEqualTo("construct");
        assertThat(pkRow.getReferencedTable()).isNull();

        ConstraintDTO fkRow = rows.get(1);
        assertThat(fkRow.getName()).isEqualTo("fk_order_customer");
        assertThat(fkRow.getType()).isEqualTo("foreign");
        assertThat(fkRow.getConstraintLevelCheck()).isEqualTo("none");
        assertThat(fkRow.getReferencedTable()).isEqualTo("demo.public.customer_t");
        assertThat(fkRow.getReferenceConstraintName()).isEqualTo("pk_customer");
        assertThat(fkRow.getOnDelete()).isEqualTo("CASCADE");
        assertThat(fkRow.getOnUpdate()).isEqualTo("NO ACTION");
    }

    @Test
    void returnsEmptyConstraintsWhenDataSetHasNoConstraints() {
        ConfigRestClientApi api = mock(ConfigRestClientApi.class);
        when(api.getDataSetDTO("order_processing")).thenReturn(dataSet("order_processing", "demo", "public", "order_t"));

        List<ConstraintDTO> rows = new CatalogService(api).getConstraints("order_processing");

        assertThat(rows).isEmpty();
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
