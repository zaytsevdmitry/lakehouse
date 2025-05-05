package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.DataSetDTO;

import org.lakehouse.config.service.DataSetService;
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
	DataSetDTO get(@PathVariable String name) {
		return dataSetService.findById(name);
	}

	@DeleteMapping(Endpoint.DATA_SETS_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		dataSetService.deleteById(name);
	}
}
