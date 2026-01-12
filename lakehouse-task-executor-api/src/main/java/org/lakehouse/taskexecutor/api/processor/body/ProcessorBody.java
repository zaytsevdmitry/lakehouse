package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskFailedException;

public interface ProcessorBody extends BodyParam {
    void run() throws TaskFailedException;

}

