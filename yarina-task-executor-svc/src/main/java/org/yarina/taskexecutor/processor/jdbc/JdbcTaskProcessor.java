/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yarina.taskexecutor.processor.jdbc;

import tools.jackson.core.JacksonException;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.exception.TaskFailedException;
import org.yarina.client.api.utils.ObjectMapping;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.taskexecutor.api.processor.TaskProcessor;
import org.yarina.taskexecutor.api.processor.body.ProcessorBody;
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
        validateTask(scheduledTaskDTO);
        ProcessorBody b = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
        b.run(scheduledTaskDTO);
    }

    private void validateTask(ScheduledTaskDTO scheduledTaskDTO) throws  TaskConfigurationException {

        if (scheduledTaskDTO.getDriverKeyName() == null || scheduledTaskDTO.getDriverKeyName().isBlank()){
            try {
                if (scheduledTaskDTO.getSqlTemplate() == null || ObjectMapping.asMap(scheduledTaskDTO.getSqlTemplate()).isEmpty()) {
                    throw new TaskConfigurationException(
                            String.format(
                                    "Task configuration error. driverKeyName or sqlTemplate must be present in the task configuration %s.%s.%s",
                                    scheduledTaskDTO.getScheduleKeyName(),
                                    scheduledTaskDTO.getScenarioActKeyName(),
                                    scheduledTaskDTO.getName()
                                    ));
                }
            }catch (JacksonException e){
                throw new TaskConfigurationException(e);
            }
        }


    }
}
