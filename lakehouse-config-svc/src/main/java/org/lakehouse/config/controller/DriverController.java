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
