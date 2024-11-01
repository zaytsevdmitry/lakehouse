package lakehouse.api.controller.configs;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.configs.ScenarioActTemplateDTO;
import lakehouse.api.service.configs.ScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScenarioController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScenarioService scenarioService;

	public ScenarioController(ScenarioService scenarioService) {
		this.scenarioService = scenarioService;
	}

	@GetMapping(Endpoint.SCENARIOS)
	List<ScenarioActTemplateDTO> findAll() {
		return scenarioService.findAll();
	}

	@PostMapping(Endpoint.SCENARIOS)
	@ResponseStatus(HttpStatus.CREATED)
	ScenarioActTemplateDTO post(@RequestBody ScenarioActTemplateDTO scenarioActTemplateDTO) {
		return scenarioService.save(scenarioActTemplateDTO);
	}

	@GetMapping(Endpoint.SCENARIOS_NAME)
	ScenarioActTemplateDTO get(@PathVariable String name) {
		return scenarioService.findById(name);
	}

	@DeleteMapping(Endpoint.SCENARIOS_NAME)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {

		scenarioService.deleteById(name);
	}
}
