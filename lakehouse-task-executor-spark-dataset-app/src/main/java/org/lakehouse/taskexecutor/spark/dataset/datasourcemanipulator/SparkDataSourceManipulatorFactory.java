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

package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactoryImpl;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtilsImpl;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameterImpl;
import org.springframework.stereotype.Service;

@Service
public class SparkDataSourceManipulatorFactory implements DataSourceManipulatorFactory{
    private final SparkSession sparkSession;
    public SparkDataSourceManipulatorFactory(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }
    @Override
    public DataSourceManipulator buildDataSourceManipulator(DriverDTO targetDriver, DataSourceDTO targetDataSource, DataSetDTO targetDataSet, JinJavaUtils jinJavaUtils, ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        DataSourceManipulator result = null;
        if (targetDriver.getDataSourceType().equals(Types.DataSourceType.database)){
            result = new DataSourceManipulatorFactoryImpl().buildDataSourceManipulator(
                    targetDriver,targetDataSource,targetDataSet,jinJavaUtils,configRestClientApi
            );// JdbcDataSourceManipulator(parameter);//JdbcSparkSQLDataSourceManipulator(parameter);
        }else {

            SparkExecuteUtils executeUtils = new SparkExecuteUtilsImpl(jinJavaUtils, targetDataSource, targetDriver, sparkSession);
            SQLTemplateDTO sqlTemplateDTO = SQLTemplateFactory.mergeSqlTemplate(targetDriver,targetDataSource,targetDataSet);

            SparkSQLDataSourceManipulatorParameter parameter = null;
            parameter = new SparkSQLDataSourceManipulatorParameterImpl(sparkSession,executeUtils,
                    new SQLTemplateResolver(configRestClientApi,sqlTemplateDTO),
                    targetDataSet);


            if (targetDriver.getDataSourceType().equals(Types.DataSourceType.iceberg)) {
                result = new IcebergSparkSQLDataSourceManipulator(parameter);
            } else if (targetDriver.getDataSourceType().equals(Types.DataSourceType.file)) {
                result = new FileSparkSQLDataSourceManipulator(parameter);
            } else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "Driver %s unsupported. Wrong DataSourceType %s",
                                targetDriver.getKeyName(),
                                targetDriver.getDataSourceType()
                        ));
            }
        }
        return result;
    }
}
