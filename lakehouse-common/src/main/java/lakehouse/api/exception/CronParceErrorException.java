package lakehouse.api.exception;

public class CronParceErrorException extends Exception{
    public CronParceErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
