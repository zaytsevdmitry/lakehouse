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
    DataSourceDTO get(@PathVariable String name) {
        return dataSourceService.findById(name);
    }

    @DeleteMapping(Endpoint.DATA_SOURCES_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        dataSourceService.deleteById(name);
    }
}
