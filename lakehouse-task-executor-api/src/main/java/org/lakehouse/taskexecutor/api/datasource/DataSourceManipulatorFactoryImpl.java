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

