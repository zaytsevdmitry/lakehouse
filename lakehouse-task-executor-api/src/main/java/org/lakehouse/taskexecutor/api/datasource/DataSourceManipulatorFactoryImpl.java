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

package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.jdbc.JdbcExecuteUtils;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;

public class DataSourceManipulatorFactoryImpl implements DataSourceManipulatorFactory{

    public  DataSourceManipulator buildDataSourceManipulator(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO,
            JinJavaUtils jinJavaUtils,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        //still one way
        ExecuteUtils jdbcUtils = new JdbcExecuteUtils(
                jinJavaUtils,
                dataSourceDTO,
                driverDTO);

        SQLTemplateDTO sqlTemplateDTO = SQLTemplateFactory.mergeSqlTemplate(
                driverDTO,
                dataSourceDTO,
                dataSetDTO);

        DataSourceManipulatorParameter parameter = new DataSourceManipulatorParameterImpl(
                jdbcUtils,
                new SQLTemplateResolver(configRestClientApi,sqlTemplateDTO),
                dataSetDTO);

        return new  JdbcDataSourceManipulator(parameter);
    }

}

