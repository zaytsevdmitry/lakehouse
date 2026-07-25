package org.lakehouse.task.proxy.spark.exception;

public class CreateErrorException extends Exception{
    public CreateErrorException(String message) {
        super(message);
    }

    public CreateErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
