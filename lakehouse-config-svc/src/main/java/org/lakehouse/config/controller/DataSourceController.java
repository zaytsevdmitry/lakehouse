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
