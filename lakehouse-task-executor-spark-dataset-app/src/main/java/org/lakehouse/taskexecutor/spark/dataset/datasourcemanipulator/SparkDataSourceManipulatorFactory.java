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
