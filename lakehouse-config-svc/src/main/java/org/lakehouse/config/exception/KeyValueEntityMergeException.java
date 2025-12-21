package org.lakehouse.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class KeyValueEntityMergeException extends RuntimeException {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String msgTemplate = "Merge property error with name  %s  %s";
    public KeyValueEntityMergeException(String propertyName, String errmsg, Throwable cause) {

        super(String.format(msgTemplate, propertyName, errmsg),cause);
        logger.error(String.format(msgTemplate, propertyName, errmsg));
    }

}
