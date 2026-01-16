package org.lakehouse.taskexecutor;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataManipulators {
    public static SparkSQLDataSourceManipulator getIcebergDataSourceManipulator(
            Jinjava jinjava,
            SparkSession sparkSession,
            String dataSetKeyName,
            TaskProcessorConfigDTO taskProcessorConfigDTO) throws IOException,  UnsuportedDataSourceException {

        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession, jinjava);
        DataSetDTO dataSetDTO = taskProcessorConfigDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = taskProcessorConfigDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());
        new CatalogActivator(sparkSession,jinjava).activate(driverDTO,dataSourceDTO);
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO);

    }

        public static SparkSQLDataSourceManipulator getSparkSQLDataSourceManipulatorPg(
            Jinjava jinjava,
            PostgreSQLContainer<?> postgres,
            SparkSession sparkSession,
            String dataSetKeyName,
            TaskProcessorConfigDTO taskProcessorConfigDTO) throws IOException, UnsuportedDataSourceException {
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession, jinjava);

        DataSetDTO dataSetDTO = taskProcessorConfigDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = taskProcessorConfigDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());
        // pg dynamic test properties
        Map<String,String> props = new HashMap<>();
        props.putAll(Map.of(
                "spark.sql.catalog.processingdb", "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
                "spark.sql.catalog.processingdb.url", postgres.getJdbcUrl(), // Replace with your JDBC URL
                "spark.sql.catalog.processingdb.user", postgres.getUsername(), // Replace with your username
                "spark.sql.catalog.processingdb.password", postgres.getPassword() // Replace with your password
        ));

        dataSourceDTO.setProperties(props);
        dataSourceDTO.getServices().get(0).setHost(postgres.getHost());
        dataSourceDTO.getServices().get(0).setPort(postgres.getMappedPort(5432).toString());
        dataSourceDTO.getServices().get(0).setUrn(postgres.getDatabaseName());
        dataSourceDTO.getServices().get(0).getProperties().put("user", postgres.getUsername());
        dataSourceDTO.getServices().get(0).getProperties().put("password", postgres.getPassword());

        new CatalogActivator(sparkSession,jinjava).activate(driverDTO,dataSourceDTO);
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO);
    }
}
