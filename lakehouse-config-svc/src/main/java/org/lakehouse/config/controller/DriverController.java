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
    DriverDTO get(@PathVariable String name) {
        return driverService.findById(name);
    }

    @DeleteMapping(Endpoint.DRIVERS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        driverService.deleteById(name);
    }
}
