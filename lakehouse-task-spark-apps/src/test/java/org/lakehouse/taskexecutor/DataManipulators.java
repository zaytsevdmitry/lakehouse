package org.lakehouse.taskexecutor;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.TableDialectFactory;
import org.lakehouse.taskexecutor.api.factory.TaskConfigBuildException;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameterImpl;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.postgres.PostgresJdbcSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.IcebergSparkDataSourceManipulator;
import org.lakehouse.test.config.configuration.FileLoader;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataManipulators {
    FileLoader fileLoader = new FileLoader();
    TaskConfigTestFactory taskConfigTestFactory = new TaskConfigTestFactory();


    private DataSourceManipulatorParameter getDataSourceManipulatorParameter(String keyName, SparkSession sparkSession)
            throws TaskConfigBuildException, IOException, TaskFailedException {
        TaskProcessorConfigDTO t = taskConfigTestFactory.loadTaskProcessorConfigDTO(keyName,"load");
        DataSetDTO dataSetDTO = t.getTargetDataSet();
        DataSourceDTO dataSourceDTO = t.getTargetDataSourceDTO();

        Map<String,DataSetDTO> foreignDataSets = t.getForeignDataSetDTOMap();
        return  new DataSourceManipulatorParameterImpl(
                sparkSession,
                dataSourceDTO,
                dataSetDTO,
                new TableDialectFactory().buildTableDialect(dataSourceDTO,dataSetDTO, foreignDataSets),
                t.getKeyBind()
        );
    }
    public  DataSourceManipulator getIcebergDataSourceManipulator(
            String keyName,
            SparkSession sparkSession) throws TaskFailedException, IOException, TaskConfigBuildException {
        DataSourceManipulatorParameter parameter = getDataSourceManipulatorParameter(keyName,sparkSession);
        new CatalogActivator(sparkSession).activate(parameter.getDataSourceDTO());
        return new IcebergSparkDataSourceManipulator(parameter);
    }

    public  DataSourceManipulator getPgDataSourceManipulator(
            String keyName,
            SparkSession sparkSession,
            PostgreSQLContainer<?> postgres) throws IOException, TaskFailedException, TaskConfigBuildException {

        DataSourceManipulatorParameter parameter = getDataSourceManipulatorParameter(keyName,sparkSession);

        DataSourceDTO dataSourceDTO = parameter.getDataSourceDTO();
        DataSetDTO dataSetDTO = parameter.getDataSetDTO();

        // pg dynamic test properties
        Map<String,String> props = new HashMap<>();
        props.putAll(Map.of(
                "spark.sql.catalog.processingdb", "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
                "spark.sql.catalog.processingdb.url", postgres.getJdbcUrl(), // Replace with your JDBC URL
                "spark.sql.catalog.processingdb.user", postgres.getUsername(), // Replace with your username
                "spark.sql.catalog.processingdb.password", postgres.getPassword() // Replace with your password
        ));
        parameter.getDataSourceDTO().getProperties().putAll(props);

        dataSourceDTO.setProperties(props);
        dataSourceDTO.getServices().get(0).setHost(postgres.getHost());
        dataSourceDTO.getServices().get(0).setPort(postgres.getMappedPort(5432).toString());
        dataSourceDTO.getServices().get(0).setUrn(postgres.getDatabaseName());
        dataSourceDTO.getServices().get(0).getProperties().put("user", postgres.getUsername());
        dataSourceDTO.getServices().get(0).getProperties().put("password", postgres.getPassword());

        new CatalogActivator(sparkSession).activate(parameter.getDataSourceDTO());
        return new PostgresJdbcSparkDataSourceManipulator(parameter);
    }
}
