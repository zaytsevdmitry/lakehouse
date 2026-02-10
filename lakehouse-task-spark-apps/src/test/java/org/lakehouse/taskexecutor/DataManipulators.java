package org.lakehouse.taskexecutor;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            JinJavaUtils jinJavaUtils,
            SparkSession sparkSession,
            String dataSetKeyName,
            SourceConfDTO sourceConfDTO) throws IOException,  UnsuportedDataSourceException {

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
            SourceConfDTO sourceConfDTO) throws IOException, UnsuportedDataSourceException {
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession, jinJavaUtils);

        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = sourceConfDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());
        // pg dynamic test properties
        Map<String,String> props = new HashMap<>();
        props.putAll(Map.of(
                "spark.sql.catalog.processingdb", "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
                "spark.sql.catalog.processingdb.url", postgres.getJdbcUrl(),
                "spark.sql.catalog.processingdb.user", postgres.getUsername(),
                "spark.sql.catalog.processingdb.password", postgres.getPassword()
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
