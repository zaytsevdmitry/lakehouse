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
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.ui.dto.CatalogTreeNodeDTO;
import org.lakehouse.ui.dto.ConstraintDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CatalogService {

    private final ConfigRestClientApi configRestClientApi;

    public CatalogService(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public List<CatalogTreeNodeDTO> getCatalogTree() {
        List<DataSetDTO> dataSets = configRestClientApi.getDataSetDTOList();

        Map<String, CatalogTreeNodeDTO> dataSourceByKeyName = new LinkedHashMap<>();
        for (DataSetDTO dataSet : dataSets) {
            dataSourceByKeyName
                    .computeIfAbsent(dataSet.getDataSourceKeyName(), keyName -> {
                        CatalogTreeNodeDTO node = new CatalogTreeNodeDTO();
                        node.setKeyName(keyName);
                        return node;
                    });
        }

        for (DataSetDTO dataSet : dataSets) {
            CatalogTreeNodeDTO dataSourceNode = dataSourceByKeyName.get(dataSet.getDataSourceKeyName());
            if (dataSourceNode == null) {
                continue;
            }
            CatalogTreeNodeDTO schemaNode = dataSourceNode.getChildren().stream()
                    .filter(node -> Objects.equals(node.getDatabaseSchemaName(), dataSet.getDatabaseSchemaName()))
                    .findFirst()
                    .orElse(null);
            if (schemaNode == null) {
                schemaNode = new CatalogTreeNodeDTO();
                schemaNode.setKeyName(dataSet.getDatabaseSchemaName());
                schemaNode.setDatabaseSchemaName(dataSet.getDatabaseSchemaName());
                schemaNode.setDataSourceKeyName(dataSet.getDataSourceKeyName());
                dataSourceNode.getChildren().add(schemaNode);
            }
            CatalogTreeNodeDTO tableNode = new CatalogTreeNodeDTO();
            tableNode.setKeyName(dataSet.getKeyName());
            tableNode.setTableName(dataSet.getTableName());
            tableNode.setDatabaseSchemaName(dataSet.getDatabaseSchemaName());
            tableNode.setDataSourceKeyName(dataSet.getDataSourceKeyName());
            schemaNode.getChildren().add(tableNode);
        }

        List<CatalogTreeNodeDTO> result = new ArrayList<>(dataSourceByKeyName.values());
        for (CatalogTreeNodeDTO dataSourceNode : result) {
            dataSourceNode.getChildren().sort(Comparator.comparing(
                    CatalogTreeNodeDTO::getDatabaseSchemaName, Comparator.nullsLast(String::compareTo)));
            for (CatalogTreeNodeDTO schemaNode : dataSourceNode.getChildren()) {
                schemaNode.setBadge(schemaNode.getChildren().size());
                schemaNode.getChildren().sort(
                        Comparator.comparing(CatalogTreeNodeDTO::getTableName, Comparator.nullsLast(String::compareTo))
                                .thenComparing(CatalogTreeNodeDTO::getKeyName, Comparator.nullsLast(String::compareTo)));
            }
            dataSourceNode.setBadge(dataSourceNode.getChildren().size());
        }
        result.sort(Comparator.comparing(CatalogTreeNodeDTO::getKeyName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    public DataSetDTO getDataSet(String keyName) {
        return configRestClientApi.getDataSetDTO(keyName);
    }

    public DataSetLineageDTO getLineage(String keyName) {
        return configRestClientApi.getDataSetLineageDTO(keyName);
    }

    public List<ConstraintDTO> getConstraints(String keyName) {
        DataSetDTO dataSet = configRestClientApi.getDataSetDTO(keyName);
        if (dataSet == null || dataSet.getConstraints() == null) {
            return List.of();
        }

        List<ConstraintDTO> result = new ArrayList<>();
        dataSet.getConstraints().forEach((name, constraint) -> {
            ConstraintDTO dto = new ConstraintDTO();
            dto.setName(name);
            if (constraint.getType() != null) {
                dto.setType(constraint.getType().toString());
            }
            dto.setColumns(constraint.getColumns());
            dto.setEnabled(constraint.isEnabled());
            if (constraint.getConstraintLevelCheck() != null) {
                dto.setConstraintLevelCheck(constraint.getConstraintLevelCheck().toString());
            }
            dto.setCheckExpr(constraint.getCheckExpr());
            dto.setTableConstraintDDLCreateOverride(constraint.getTableConstraintDDLCreateOverride());
            dto.setTableConstraintDDLAddOverride(constraint.getTableConstraintDDLAddOverride());

            ForeignKeyReferenceDTO reference = constraint.getReference();
            if (reference != null) {
                dto.setReferenceConstraintName(reference.getConstraintName());
                if (reference.getOnDelete() != null) {
                    dto.setOnDelete(reference.getOnDelete().getValue());
                }
                if (reference.getOnUpdate() != null) {
                    dto.setOnUpdate(reference.getOnUpdate().getValue());
                }
                DataSetDTO referenced = configRestClientApi.getDataSetDTO(reference.getDataSetKeyName());
                if (referenced != null) {
                    dto.setReferencedTable(joinKey(
                            referenced.getDataSourceKeyName(),
                            referenced.getDatabaseSchemaName(),
                            referenced.getTableName()));
                }
            }
            result.add(dto);
        });
        return result;
    }

    private String joinKey(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
