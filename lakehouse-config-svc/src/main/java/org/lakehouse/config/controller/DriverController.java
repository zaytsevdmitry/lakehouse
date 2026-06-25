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
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.config.service.datasource.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping(Endpoint.DRIVERS)
    List<DriverDTO> all() {
        return driverService.findAll();
    }

    @PostMapping(Endpoint.DRIVERS)
    @ResponseStatus(HttpStatus.CREATED)
    DriverDTO put(@RequestBody DriverDTO dataSourceDTO) {
        return driverService.save(dataSourceDTO);
    }

    @GetMapping(Endpoint.DRIVERS_NAME)
    DriverDTO get(@PathVariable String keyName) {
        return driverService.findById(keyName);
    }

    @DeleteMapping(Endpoint.DRIVERS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        driverService.deleteById(keyName);
    }
}
