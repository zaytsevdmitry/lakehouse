package org.lakehouse.client.api.dto.task;

import org.lakehouse.client.api.exception.TaskFailedException;


public interface TaskProcessor {

    void runTask() throws TaskFailedException;
	
}
