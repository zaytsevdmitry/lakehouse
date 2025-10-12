package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class QualityMetricsConfNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 94297551951697828L;

    public QualityMetricsConfNotFoundException(String name) {
        super(String.format("DataSet with name %s not found", name));
    }

}
