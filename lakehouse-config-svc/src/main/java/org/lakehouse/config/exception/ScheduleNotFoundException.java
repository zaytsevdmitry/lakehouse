package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScheduleNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1955935669950721393L;

	public ScheduleNotFoundException(String name) {
		super(String.format("Schedule with name %s not found", name));
	}

}
