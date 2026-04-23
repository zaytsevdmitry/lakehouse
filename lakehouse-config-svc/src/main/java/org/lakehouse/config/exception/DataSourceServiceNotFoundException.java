package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSourceServiceNotFoundException extends RuntimeException {

    public DataSourceServiceNotFoundException(String name) {
        super(String.format("Service of DataSource with name %s not found", name));
    }

}
