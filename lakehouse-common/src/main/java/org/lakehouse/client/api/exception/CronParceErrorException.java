package org.lakehouse.client.api.exception;

import java.io.Serial;

public class CronParceErrorException extends Exception {
    @Serial
    private static final long serialVersionUID = -5903835227447756409L;

    public CronParceErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
