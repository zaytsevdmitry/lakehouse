package org.lakehouse.config.validator;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;


@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class ConfDTOValidationException extends RuntimeException {

    public ConfDTOValidationException(List<String> strings) {

        super(String.join("\n", strings));
    }

}
