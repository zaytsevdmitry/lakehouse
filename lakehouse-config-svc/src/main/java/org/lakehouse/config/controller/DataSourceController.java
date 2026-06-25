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

package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.config.service.datasource.DataSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DataSourceController {
    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping(Endpoint.DATA_SOURCES)
    List<DataSourceDTO> all() {
        return dataSourceService.findAll();
    }

    @PostMapping(Endpoint.DATA_SOURCES)
    @ResponseStatus(HttpStatus.CREATED)
    DataSourceDTO put(@RequestBody DataSourceDTO dataSourceDTO) {
        return dataSourceService.save(dataSourceDTO);
    }

    @GetMapping(Endpoint.DATA_SOURCES_NAME)
    DataSourceDTO get(@PathVariable String keyName) {
        return dataSourceService.findById(keyName);
    }

    @DeleteMapping(Endpoint.DATA_SOURCES_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        dataSourceService.deleteById(keyName);
    }
}
