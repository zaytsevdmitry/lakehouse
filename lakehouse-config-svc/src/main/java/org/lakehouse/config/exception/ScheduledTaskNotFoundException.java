package org.lakehouse.config.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScheduledTaskNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -613204078471058463L;

	public ScheduledTaskNotFoundException(Long id) {
		super(String.format("Schedule with id %d not found", id));
	}
	public ScheduledTaskNotFoundException(String taskExecutionServiceGroup , String status) {
		super(String.format("Schedule with taskExecutionServiceGroup %s and status %s not found" , taskExecutionServiceGroup, status));
	}

}
