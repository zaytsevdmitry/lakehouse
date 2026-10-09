/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.taskexecutor.api.datasource;

import org.jarina.client.api.dto.common.SQLTemplateDTO;
import org.jarina.client.api.dto.configs.dataset.DataSetDTO;
import org.jarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.jarina.client.api.exception.TaskConfigurationException;
import org.jarina.client.rest.config.ConfigRestClientApi;
import org.jarina.jinja.java.JinJavaUtils;
import org.jarina.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.jarina.taskexecutor.api.datasource.execute.jdbc.JdbcExecuteUtils;
import org.jarina.taskexecutor.api.facade.SQLTemplateResolver;

public class DataSourceManipulatorFactoryImpl implements DataSourceManipulatorFactory{

    public  DataSourceManipulator buildDataSourceManipulator(
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO,
            SQLTemplateDTO sqlTemplateDTO,
            JinJavaUtils jinJavaUtils,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        //still one way
        ExecuteUtils jdbcUtils = new JdbcExecuteUtils(
                jinJavaUtils,
                dataSourceDTO);


        DataSourceManipulatorParameter parameter = new DataSourceManipulatorParameterImpl(
                jdbcUtils,
                new SQLTemplateResolver(configRestClientApi,sqlTemplateDTO),
                dataSetDTO);

        return new  JdbcDataSourceManipulator(parameter);
    }

}

