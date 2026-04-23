package org.lakehouse.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSetConstraintReferenceConfigNotFoundException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long serialVersionUID = 94297551951697828L;
    private static final String msgTemplate = "Config error. Reference not found in constraint with name  %s  dataset %s";
    public DataSetConstraintReferenceConfigNotFoundException(String constraintName, String datasetName) {

        super(String.format(msgTemplate, constraintName, datasetName));
        logger.error(String.format(msgTemplate, constraintName, datasetName));
    }

}
