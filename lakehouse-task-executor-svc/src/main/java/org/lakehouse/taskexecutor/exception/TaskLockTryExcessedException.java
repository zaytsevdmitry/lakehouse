package org.lakehouse.taskexecutor.exception;

public class TaskLockTryExcessedException extends Exception {

	public TaskLockTryExcessedException() {
		super();
	}

	public TaskLockTryExcessedException(int tryNum) {
		super(String.format("%d  attempts were made", tryNum));
	}
}
