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

import tools.jackson.core.JacksonException;
import org.yarina.client.api.dto.common.SQLTemplateDTO;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.taskexecutor.api.factory.SQLTemplateFactory;
import org.yarina.client.api.utils.ObjectMapping;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.jinja.java.JinJavaFactory;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.taskexecutor.api.datasource.DataSourceManipulator;
import org.yarina.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.yarina.taskexecutor.api.processor.body.ProcessorBody;

public abstract class SQLProcessorBodyAbstract implements ProcessorBody{
    private final ConfigRestClientApi configRestClientApi;
    private final DataSourceManipulatorFactory dataSourceManipulatorFactory;
    private final SQLTemplateFactory sqlTemplateFactory;
    protected SQLProcessorBodyAbstract(
            ConfigRestClientApi configRestClientApi,
            DataSourceManipulatorFactory dataSourceManipulatorFactory, SQLTemplateFactory sqlTemplateFactory) {
        this.configRestClientApi = configRestClientApi;
        this.dataSourceManipulatorFactory = dataSourceManipulatorFactory;
        this.sqlTemplateFactory = sqlTemplateFactory;
    }

    public DataSourceManipulator getTargetDataSourceManipulator(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskConfigurationException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());

        SQLTemplateDTO sqlTemplateDTO = sqlTemplateFactory.mergeSqlTemplate(scheduledTaskDTO);

        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        try {
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

        } catch (JacksonException e) {
            throw new TaskConfigurationException(e);
        }
        return dataSourceManipulatorFactory
                .buildDataSourceManipulator(
                        sourceConfDTO.getTargetDataSource(),
                        sourceConfDTO.getTargetDataSet(),
                        sqlTemplateDTO,
                        jinJavaUtils,
                        configRestClientApi);
    }
}
