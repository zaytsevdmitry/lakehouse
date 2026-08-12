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
package org.lakehouse.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class CatalogTreeNodeDTO {

    private String keyName;
    private String dataSourceKeyName;
    private String databaseSchemaName;
    private String tableName;
    private Integer badge;
    private List<CatalogTreeNodeDTO> children = new ArrayList<>();

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getDataSourceKeyName() {
        return dataSourceKeyName;
    }

    public void setDataSourceKeyName(String dataSourceKeyName) {
        this.dataSourceKeyName = dataSourceKeyName;
    }

    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public List<CatalogTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<CatalogTreeNodeDTO> children) {
        this.children = children;
    }
}
