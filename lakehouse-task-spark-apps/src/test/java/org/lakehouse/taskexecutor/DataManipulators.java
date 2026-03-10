package org.lakehouse.taskexecutor;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.processor.body.body.CatalogActivator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            JinJavaUtils jinJavaUtils,
            SparkSession sparkSession,
            String dataSetKeyName,
            SourceConfDTO sourceConfDTO) throws TaskConfigurationException {

        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession, jinJavaUtils);
        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = sourceConfDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());

        new CatalogActivator(
                sparkSession)
                .activate(List.of(dataSourceDTO));
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO);

    }

        public static DataSourceManipulator getSparkSQLDataSourceManipulatorPg(
            JinJavaUtils jinJavaUtils,
            PostgreSQLContainer<?> postgres,
            SparkSession sparkSession,
            String dataSetKeyName,
            SourceConfDTO sourceConfDTO) throws  TaskConfigurationException {
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession, jinJavaUtils);

        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = sourceConfDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());
        // pg dynamic test properties
        Map<String,String> props = new HashMap<>();
        props.putAll(Map.of(
                "spark.sql.catalog.processing", "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
                "spark.sql.catalog.processing.url", postgres.getJdbcUrl(),
                "spark.sql.catalog.processing.user", postgres.getUsername(),
                "spark.sql.catalog.processing.password", postgres.getPassword()
        ));
        props.forEach((k, v) -> sparkSession.conf().set(k,v));

        dataSourceDTO.getService().setProperties(props);
        dataSourceDTO.getService().setHost(postgres.getHost());
        dataSourceDTO.getService().setPort(postgres.getMappedPort(5432).toString());
        dataSourceDTO.getService().setUrn(postgres.getDatabaseName());
        dataSourceDTO.getService().getProperties().put("user", postgres.getUsername());
        dataSourceDTO.getService().getProperties().put("password", postgres.getPassword());

        new CatalogActivator(
                sparkSession)
                .activate(List.of(dataSourceDTO));
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO);
    }
}
