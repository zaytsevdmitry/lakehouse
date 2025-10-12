package org.lakehouse.taskexecutor.entity;

import org.lakehouse.taskexecutor.exception.TaskFailedException;

public interface TaskProcessor {

    void runTask() throws TaskFailedException;

}
