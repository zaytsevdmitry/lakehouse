package org.lakehouse.config.exception;

public class TaskExecutionServiceGroupNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 957383273295670751L;

	public TaskExecutionServiceGroupNotFoundException(String name) {
		super(String.format("Project with name %s not found", name));
	}

}
