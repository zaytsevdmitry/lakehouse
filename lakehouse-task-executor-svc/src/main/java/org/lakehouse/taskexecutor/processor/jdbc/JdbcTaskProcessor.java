package org.lakehouse.taskexecutor.processor.jdbc;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service(value = "jdbcTaskProcessor")
public  class JdbcTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConfigurableApplicationContext applicationContext;

    public JdbcTaskProcessor(
            ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {
        logger.info("Making JDBC command class body instance {}", scheduledTaskDTO.getTaskProcessorBody());

        ProcessorBody b = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
        b.run(scheduledTaskDTO);
    }
}
