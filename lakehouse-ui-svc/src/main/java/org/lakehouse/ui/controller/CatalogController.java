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
package org.lakehouse.ui.controller;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.ui.dto.CatalogTreeNodeDTO;
import org.lakehouse.ui.dto.ConstraintDTO;
import org.lakehouse.ui.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/tree")
    public List<CatalogTreeNodeDTO> getCatalogTree() {
        return catalogService.getCatalogTree();
    }

    @GetMapping("/dataset/{keyName}")
    public DataSetDTO getDataSet(@PathVariable String keyName) {
        return catalogService.getDataSet(keyName);
    }

    @GetMapping("/dataset/{keyName}/lineage")
    public DataSetLineageDTO getLineage(@PathVariable String keyName) {
        return catalogService.getLineage(keyName);
    }

    @GetMapping("/dataset/{keyName}/constraints")
    public List<ConstraintDTO> getConstraints(@PathVariable String keyName) {
        return catalogService.getConstraints(keyName);
    }

    @GetMapping("/script/{key}")
    public String getScript(@PathVariable String key) {
        return catalogService.getScript(key);
    }

    @GetMapping("/dataset/{keyName}/model-script")
    public String getDataSetModelScript(@PathVariable String keyName) {
        return catalogService.getDataSetModelScript(keyName);
    }

    @GetMapping("/datasource/{keyName}")
    public DataSourceDTO getDataSource(@PathVariable String keyName) {
        return catalogService.getDataSource(keyName);
    }
}
