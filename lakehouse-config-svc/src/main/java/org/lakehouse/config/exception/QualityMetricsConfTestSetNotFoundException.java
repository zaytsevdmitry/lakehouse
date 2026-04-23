package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class QualityMetricsConfTestSetNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -867838510444625910L;

    public QualityMetricsConfTestSetNotFoundException(String msg) {
        super(msg);
    }

}
