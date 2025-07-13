package org.lakehouse.taskexecutor.executionmodule;
import com.hubspot.jinjava.Jinjava;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class AbstractTaskProcessor implements TaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfig taskProcessorConfig;
	private final Jinjava jinjava;
	public AbstractTaskProcessor(
            TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
		this.taskProcessorConfig = taskProcessorConfig;
        this.jinjava = jinjava;
    }

	public TaskProcessorConfig getTaskProcessorConfig() {
		return taskProcessorConfig;
	}

	public Jinjava getJinjava() {
		return jinjava;
	}
}
