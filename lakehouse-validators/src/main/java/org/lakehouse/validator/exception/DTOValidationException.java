package org.lakehouse.validator.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;


@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
public class DTOValidationException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DTOValidationException(List<String> strings) {

        super(String.join("\n", strings));
        logger.error(strings.stream().collect(Collectors.joining("\n")));

    }

}
