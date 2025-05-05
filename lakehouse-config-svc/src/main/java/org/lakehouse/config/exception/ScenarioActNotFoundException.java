package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScenarioActNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1955935669950721393L;

	public ScenarioActNotFoundException(String scheduleName,String scenarioName) {
		super(String.format("Schedule scenario with name %s.%s not found", scheduleName,scenarioName));
	}

}
