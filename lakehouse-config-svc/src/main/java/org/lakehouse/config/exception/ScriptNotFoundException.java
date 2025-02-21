package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScriptNotFoundException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 7022767127419305773L;

	public ScriptNotFoundException(String key) {
		super(String.format("Script with key %s not found", key));
	}

}
