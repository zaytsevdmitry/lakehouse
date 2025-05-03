package org.lakehouse.taskexecutor.exception;

public class TaskLockNotFoundException extends Exception {

	public TaskLockNotFoundException() {
		super();
	}

	public TaskLockNotFoundException(Throwable cause) {
		super(cause);
	}
}
