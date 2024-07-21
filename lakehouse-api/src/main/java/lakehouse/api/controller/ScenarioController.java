package lakehouse.api.controller;

import lakehouse.api.dto.ScenarioDTO;
import lakehouse.api.service.ScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScenarioController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }


    @GetMapping("/scenarios")
    List<ScenarioDTO> findAll() {
        return scenarioService.findAll();
    }

    @PostMapping("/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    ScenarioDTO post(@RequestBody ScenarioDTO scenarioDTO) {
        return scenarioService.save(scenarioDTO);
    }


    @GetMapping("/scenarios/{name}")
    ScenarioDTO get(@PathVariable String name) {
        return scenarioService.findById(name);
    }


    @DeleteMapping("/scenarios/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {

        scenarioService.deleteById(name);
    }
}
