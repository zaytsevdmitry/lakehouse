package org.lakehouse.taskexecutor.processor;

import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.client.api.exception.TaskFailedException;


public abstract class AbstractTaskProcessor implements TaskProcessor {
    public AbstractTaskProcessor() {
    }
    protected void sleep(long ms) throws TaskFailedException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new TaskFailedException("Sleep failed", e);
        }
    }

}
