package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskFailedException;

public interface ProcessorBody {
    void run(BodyParam bodyParam) throws TaskFailedException;

}

