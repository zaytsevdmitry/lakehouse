package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class ConfigCorruptException extends RuntimeException {
    public ConfigCorruptException(String message, Throwable cause) {
        super(message, cause);
    }public ConfigCorruptException( Throwable cause) {
        super( cause);
    }


}
