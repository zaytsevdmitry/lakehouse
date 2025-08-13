package org.lakehouse.common.api.task.processor.entity;

import org.lakehouse.common.api.task.processor.exception.TaskFailedException;


public interface TaskProcessor {

    void runTask() throws TaskFailedException;
	
}
