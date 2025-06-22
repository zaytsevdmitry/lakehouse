package org.lakehouse.taskexecutor.executionmodule;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;

public abstract class AbstractDefaultTaskProcessor extends AbstractTaskProcessor{
    public AbstractDefaultTaskProcessor(TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
        super(taskProcessorConfig, jinjava);
    }
}
