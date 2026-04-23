package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(String name) {
        super(String.format("Driver with name %s not found", name));
    }

}
