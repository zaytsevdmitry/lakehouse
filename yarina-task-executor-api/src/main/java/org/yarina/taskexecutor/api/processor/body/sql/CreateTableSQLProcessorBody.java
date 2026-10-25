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

package org.yarina.taskexecutor.api.processor.body.sql;

import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.exception.TaskFailedException;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.yarina.taskexecutor.api.datasource.exception.CreateException;
import org.yarina.taskexecutor.api.factory.SQLTemplateFactory;
import org.springframework.stereotype.Service;

@Service(value = "createTableSQLProcessorBody")
public class CreateTableSQLProcessorBody extends SQLProcessorBodyAbstract{
    public CreateTableSQLProcessorBody(
            ConfigRestClientApi configRestClientApi,
            DataSourceManipulatorFactory dataSourceManipulatorFactory,
            SQLTemplateFactory sqlTemplateFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory, sqlTemplateFactory);
    }

    @Override
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException,TaskConfigurationException {
        try {
            getTargetDataSourceManipulator(scheduledTaskDTO).createTableIfNotExists();
        } catch (CreateException e) {
            throw new TaskFailedException(e);
        }

    }


}
