package org.lakehouse.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSetConstraintNotFoundException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Serial
    private static final long serialVersionUID = 94297551951697828L;
    private static final String msgTemplate = "Constraint with name  %s not found in dataset %s";
    public DataSetConstraintNotFoundException(String constraintName, String datasetName) {

        super(String.format(msgTemplate, constraintName, datasetName));
        logger.error(String.format(msgTemplate, constraintName, datasetName));
    }

}
