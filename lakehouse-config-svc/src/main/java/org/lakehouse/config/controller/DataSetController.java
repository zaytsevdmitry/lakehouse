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
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.config.service.dataset.DataSetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DataSetController {
    private final DataSetService dataSetService;

    public DataSetController(DataSetService dataSetService) {
        this.dataSetService = dataSetService;
    }

    @GetMapping(Endpoint.DATA_SETS)
    List<DataSetDTO> getAll() {
        return dataSetService.findAll();
    }

    @PostMapping(Endpoint.DATA_SETS)
    @ResponseStatus(HttpStatus.CREATED)
    DataSetDTO post(@RequestBody DataSetDTO dataSetDTO) {
        return dataSetService.save(dataSetDTO);
    }

    @GetMapping(Endpoint.DATA_SETS_NAME)
    DataSetDTO get(@PathVariable String keyName) {
        return dataSetService.findById(keyName);
    }

    @DeleteMapping(Endpoint.DATA_SETS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        dataSetService.deleteById(keyName);
    }
}
