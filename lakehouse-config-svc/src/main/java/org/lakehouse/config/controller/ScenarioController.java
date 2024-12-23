package org.lakehouse.config.controller;

import org.lakehouse.cli.api.constant.Endpoint;
import org.lakehouse.cli.api.dto.configs.ScenarioActTemplateDTO;

import org.lakehouse.config.service.ScenarioActTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScenarioController {
	private final ScenarioActTemplateService scenarioActTemplateService;

	public ScenarioController(ScenarioActTemplateService scenarioActTemplateService) {
		this.scenarioActTemplateService = scenarioActTemplateService;
	}

	@GetMapping(Endpoint.SCENARIOS)
	List<ScenarioActTemplateDTO> findAll() {
		return scenarioActTemplateService.findAll();
	}

	@PostMapping(Endpoint.SCENARIOS)
	@ResponseStatus(HttpStatus.CREATED)
	ScenarioActTemplateDTO post(@RequestBody ScenarioActTemplateDTO scenarioActTemplateDTO) {
		return scenarioActTemplateService.save(scenarioActTemplateDTO);
	}

	@GetMapping(Endpoint.SCENARIOS_NAME)
	ScenarioActTemplateDTO get(@PathVariable String name) {
		return scenarioActTemplateService.findById(name);
	}

	@DeleteMapping(Endpoint.SCENARIOS_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {

		scenarioActTemplateService.deleteById(name);
	}
}
