package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.lakehouse.client.api.constant.Status;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ReleaseTaskStatusChangeException extends RuntimeException{
	private static final long serialVersionUID = -7358717046070125002L;

	public ReleaseTaskStatusChangeException(Status.Task status) {
	   super(String.format( "Status %s not allowed. Use one of: SUCCESS or FAILED", status.label));
	}

}
