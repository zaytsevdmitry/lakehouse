/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.springframework.stereotype.Service;

@Service(value = "appendSQLProcessorBody")
public class AppendSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    private final ConfigRestClientApi configRestClientApi;

    public AppendSQLProcessorBody(
            ConfigRestClientApi configRestClientApi,
            DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory);

        this.configRestClientApi = configRestClientApi;
    }
    @Override
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException {
        DataSourceManipulator targetDataSourceManipulator = getTargetDataSourceManipulator(scheduledTaskDTO);

        execute(
                targetDataSourceManipulator.executeUtils(),
                targetDataSourceManipulator.sqlTemplateResolver().getInsertDML(),
                configRestClientApi.getDataSetModelScript(targetDataSourceManipulator.dataSetDTO().getKeyName()));
    }

}
