package org.lakehouse.taskexecutor.entity;

import org.lakehouse.client.api.constant.Status;

// todo replace runnable to callable for take result task status
public interface TaskProcessor {

    Status.Task runTask();
	
}
