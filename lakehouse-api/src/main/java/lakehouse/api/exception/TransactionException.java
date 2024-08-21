package lakehouse.api.exception;

public class TransactionException extends org.springframework.transaction.TransactionException {
    public TransactionException(String msg) {
        super(msg);
    }

    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
