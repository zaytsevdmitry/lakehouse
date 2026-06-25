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
