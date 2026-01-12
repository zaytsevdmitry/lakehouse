package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSourceNotFoundException extends RuntimeException {

    public DataSourceNotFoundException(String name) {
        super(String.format("DataSource with name %s not found", name));
    }

}
