package org.lakehouse.validator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;


@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class DTOValidationException extends RuntimeException {

    public DTOValidationException(List<String> strings) {

        super(String.join("\n", strings));
    }

}
