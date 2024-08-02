package lakehouse.api.controller;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.ScenarioDTO;
import lakehouse.api.service.ScenarioService;
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


    @GetMapping(Endpoint.SCENARIO)
    List<ScenarioDTO> findAll() {
        return scenarioService.findAll();
    }

    @PostMapping(Endpoint.SCENARIO)
    @ResponseStatus(HttpStatus.CREATED)
    ScenarioDTO post(@RequestBody ScenarioDTO scenarioDTO) {
        return scenarioService.save(scenarioDTO);
    }


    @GetMapping(Endpoint.SCENARIO_NAME)
    ScenarioDTO get(@PathVariable String name) {
        return scenarioService.findById(name);
    }


    @DeleteMapping(Endpoint.SCENARIO_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {

        scenarioService.deleteById(name);
    }
}
