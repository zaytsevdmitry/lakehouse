package org.lakehouse.taskexecutor.exception;

public class TaskConfigurationException extends Exception {
	private static final long serialVersionUID = 1464725978028001612L;

	public TaskConfigurationException() {
		super();
	}


	public TaskConfigurationException(String msg) {
		super(msg);
	}

	public TaskConfigurationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
