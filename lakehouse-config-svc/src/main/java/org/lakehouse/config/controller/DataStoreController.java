package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import org.lakehouse.config.service.DataStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DataStoreController {
	private final DataStoreService dataStoreService;

	public DataStoreController(DataStoreService dataStoreService) {
		this.dataStoreService = dataStoreService;
	}

	@GetMapping(Endpoint.DATA_STORES)
	List<DataStoreDTO> all() {
		return dataStoreService.findAll();
	}

	@PostMapping(Endpoint.DATA_STORES)
	@ResponseStatus(HttpStatus.CREATED)
	DataStoreDTO put(@RequestBody DataStoreDTO dataStoreDTO) {
		return dataStoreService.save(dataStoreDTO);
	}

	@GetMapping(Endpoint.DATA_STORES_NAME)
	DataStoreDTO get(@PathVariable String name) {
		return dataStoreService.findById(name);
	}

	@DeleteMapping(Endpoint.DATA_STORES_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		dataStoreService.deleteById(name);
	}
}
