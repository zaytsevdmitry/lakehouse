package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScheduledTaskInstanceLockNotFoundException extends RuntimeException {
	

	private static final long serialVersionUID = 2387655522575107058L;

	public ScheduledTaskInstanceLockNotFoundException(Long id) {
		super(String.format("Lock with id %d not found", id));
	}

}
