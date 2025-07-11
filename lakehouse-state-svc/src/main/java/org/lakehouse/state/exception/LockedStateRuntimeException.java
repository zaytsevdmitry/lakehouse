package org.lakehouse.state.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class LockedStateRuntimeException extends RuntimeException {

	public LockedStateRuntimeException(String errm) {
		super(errm);
	}

}
