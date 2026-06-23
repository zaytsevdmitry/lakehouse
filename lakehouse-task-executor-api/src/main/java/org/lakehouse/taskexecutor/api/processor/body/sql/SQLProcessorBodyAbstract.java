/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.taskexecutor.api.processor.body.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;

public abstract class SQLProcessorBodyAbstract implements ProcessorBody{
    private final ConfigRestClientApi configRestClientApi;
    private final DataSourceManipulatorFactory dataSourceManipulatorFactory;
    protected SQLProcessorBodyAbstract(ConfigRestClientApi configRestClientApi, DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        this.configRestClientApi = configRestClientApi;
        this.dataSourceManipulatorFactory = dataSourceManipulatorFactory;
    }

    public DataSourceManipulator getTargetDataSourceManipulator(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskConfigurationException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        try {
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        return dataSourceManipulatorFactory
                .buildDataSourceManipulator(
                        sourceConfDTO.getTargetDriver(),
                        sourceConfDTO.getTargetDataSource(),
                        sourceConfDTO.getTargetDataSet(),
                        jinJavaUtils,
                        configRestClientApi);
    }
}
