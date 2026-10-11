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

package org.jarina.taskexecutor.spark.dataset.datasourcemanipulator;

import org.apache.spark.sql.SparkSession;
import org.jarina.client.api.constant.Types;
import org.jarina.client.api.dto.common.SQLTemplateDTO;
import org.jarina.client.api.dto.configs.dataset.DataSetDTO;
import org.jarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.jarina.client.api.exception.TaskConfigurationException;
import org.jarina.client.rest.config.ConfigRestClientApi;
import org.jarina.jinja.java.JinJavaUtils;
import org.jarina.taskexecutor.api.datasource.DataSourceManipulator;
import org.jarina.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.jarina.taskexecutor.api.datasource.DataSourceManipulatorFactoryImpl;
import org.jarina.taskexecutor.api.facade.SQLTemplateResolver;
import org.jarina.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;
import org.jarina.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtilsImpl;
import org.jarina.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.jarina.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameterImpl;
import org.springframework.stereotype.Service;

@Service
public class SparkDataSourceManipulatorFactory implements DataSourceManipulatorFactory{
    private final SparkSession sparkSession;
    public SparkDataSourceManipulatorFactory(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }
    @Override
    public DataSourceManipulator buildDataSourceManipulator(
            DataSourceDTO targetDataSource,
            DataSetDTO targetDataSet,
            SQLTemplateDTO sqlTemplateDTO,
            JinJavaUtils jinJavaUtils,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        DataSourceManipulator result = null;
        if (targetDataSource.getDataSourceType().equals(Types.DataSourceType.DATABASE)){
            result = new DataSourceManipulatorFactoryImpl().buildDataSourceManipulator(
                    targetDataSource,targetDataSet,sqlTemplateDTO,
                    jinJavaUtils,configRestClientApi
            );
        }else {

            SparkExecuteUtils executeUtils = new SparkExecuteUtilsImpl(jinJavaUtils, targetDataSource, sparkSession);

            SparkSQLDataSourceManipulatorParameter parameter = null;
            parameter = new SparkSQLDataSourceManipulatorParameterImpl(sparkSession,executeUtils,
                    new SQLTemplateResolver(configRestClientApi,sqlTemplateDTO),
                    targetDataSet);


            if (targetDataSource.getDataSourceType().equals(Types.DataSourceType.ICEBERG)) {
                result = new IcebergSparkSQLDataSourceManipulator(parameter);
            } else if (targetDataSource.getDataSourceType().equals(Types.DataSourceType.FILE)) {
                result = new FileSparkSQLDataSourceManipulator(parameter);
            } else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "Driver %s unsupported. Wrong DataSourceType %s",
                                targetDataSource.getKeyName(),
                                targetDataSource.getDataSourceType()
                        ));
            }
        }
        return result;
    }
}
