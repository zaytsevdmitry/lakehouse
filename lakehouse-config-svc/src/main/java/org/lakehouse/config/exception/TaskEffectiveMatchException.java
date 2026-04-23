package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class TaskEffectiveMatchException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -205924149544019777L;

    public TaskEffectiveMatchException(String msg) {
        super(msg);
    }
}
