package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataStoreNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -1852416899380338020L;

	public DataStoreNotFoundException(String name) {
		super(String.format("DataStore with name %s not found", name));
	}

}
