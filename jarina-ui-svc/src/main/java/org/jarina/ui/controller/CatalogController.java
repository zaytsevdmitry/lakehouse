/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.ui.controller;

import org.jarina.client.api.dto.configs.dataset.DataSetDTO;
import org.jarina.client.api.dto.configs.dataset.DataSetLineageDTO;
import org.jarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.jarina.ui.dto.CatalogTreeNodeDTO;
import org.jarina.ui.dto.ConstraintDTO;
import org.jarina.ui.service.CatalogService;
import org.springframework.security.core.Authentication;
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
    public List<CatalogTreeNodeDTO> getCatalogTree(Authentication authentication) {
        return catalogService.getCatalogTree(authentication);
    }

    @GetMapping("/dataset/{keyName}")
    public DataSetDTO getDataSet(@PathVariable String keyName, Authentication authentication) {
        return catalogService.getDataSet(keyName, authentication);
    }

    @GetMapping("/dataset/{keyName}/lineage")
    public DataSetLineageDTO getLineage(@PathVariable String keyName, Authentication authentication) {
        return catalogService.getLineage(keyName, authentication);
    }

    @GetMapping("/dataset/{keyName}/constraints")
    public List<ConstraintDTO> getConstraints(@PathVariable String keyName, Authentication authentication) {
        return catalogService.getConstraints(keyName, authentication);
    }

    @GetMapping("/script/{key}")
    public String getScript(@PathVariable String key) {
        return catalogService.getScript(key);
    }

    @GetMapping("/dataset/{keyName}/model-script")
    public String getDataSetModelScript(@PathVariable String keyName, Authentication authentication) {
        return catalogService.getDataSetModelScript(keyName, authentication);
    }

    @GetMapping("/datasource/{keyName}")
    public DataSourceDTO getDataSource(@PathVariable String keyName) {
        return catalogService.getDataSource(keyName);
    }
}
