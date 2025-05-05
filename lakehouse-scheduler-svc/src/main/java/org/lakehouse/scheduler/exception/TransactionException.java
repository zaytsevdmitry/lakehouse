package org.lakehouse.scheduler.exception;

public class TransactionException extends org.springframework.transaction.TransactionException {
	private static final long serialVersionUID = 1464725978028001612L;

	public TransactionException(String msg) {
		super(msg);
	}

	public TransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
