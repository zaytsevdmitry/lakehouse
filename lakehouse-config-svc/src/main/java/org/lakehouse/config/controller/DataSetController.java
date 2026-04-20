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
