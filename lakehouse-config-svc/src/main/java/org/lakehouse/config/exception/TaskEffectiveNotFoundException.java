package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TaskEffectiveNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7338414307650958600L;

	public TaskEffectiveNotFoundException(String scheduleName,String scenarioActName, String taskName) {
		super(String.format("Schedule scenario with name %s.%s not found", scenarioActName, taskName));
	}
}
