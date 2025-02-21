package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.config.service.ScriptService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ScriptController {
	private final ScriptService scriptService;

	public ScriptController(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	@GetMapping(Endpoint.SCRIPTS)
	Map<String, String> getAll() {
		return scriptService.findAll();
	}

	@PostMapping(Endpoint.SCRIPT_BY_KEY)
	@ResponseStatus(HttpStatus.CREATED)
	String post(@PathVariable String key,@RequestBody String value) {
		return scriptService.save(key,value);
	}

	@GetMapping(Endpoint.SCRIPT_BY_KEY)
	String get(@PathVariable String key) {
		return scriptService.findScriptBodyByKey(key);
	}

	@DeleteMapping(Endpoint.SCRIPT_BY_KEY)
	@ResponseStatus(HttpStatus.ACCEPTED)
	void deleteById(@PathVariable String name) {
		scriptService.deleteById(name);
	}
}
