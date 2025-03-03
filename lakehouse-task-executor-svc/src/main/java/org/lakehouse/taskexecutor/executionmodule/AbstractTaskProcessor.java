package org.lakehouse.taskexecutor.executionmodule;
import org.lakehouse.client.api.dto.configs.ColumnDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public abstract class AbstractTaskProcessor implements TaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfig taskProcessorConfig;
	public AbstractTaskProcessor(
			TaskProcessorConfig taskProcessorConfig) {
		this.taskProcessorConfig = taskProcessorConfig;
    }


}
