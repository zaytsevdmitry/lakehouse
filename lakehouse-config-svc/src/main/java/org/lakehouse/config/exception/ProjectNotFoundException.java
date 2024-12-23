package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ProjectNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -4990010890249024217L;

	public ProjectNotFoundException(String name) {
		super(String.format("Project with name %s not found", name));
	}

}
