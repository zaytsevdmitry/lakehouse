package org.lakehouse.taskexecutor.exception;

public class TaskProcessorConfigurationException extends TaskConfigurationException {

	public TaskProcessorConfigurationException(String msg) {
		super(msg);
	}
	public TaskProcessorConfigurationException(String msg,Throwable throwable) {
		super(msg,throwable);
	}
}
