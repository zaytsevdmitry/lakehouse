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

package org.lakehouse.taskexecutor.spark.dq.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            SparkSession sparkSession,
            String dataSetKeyName,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException, JsonProcessingException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession);
        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = sourceConfDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());

        new CatalogActivatorService(
                sparkSession)
                .activate(sourceConfDTO);
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO,jinJavaUtils,configRestClientApi);

    }

        public static DataSourceManipulator getSparkSQLDataSourceManipulatorPg(
            JinJavaUtils jinJavaUtils,
            PostgreSQLContainer<?> postgres,
            SparkSession sparkSession,
            String dataSetKeyName,
            SourceConfDTO sourceConfDTO,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession);

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

        new CatalogActivatorService(
                sparkSession)
                .activate(sourceConfDTO);
        return manipulatorFactory.buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO,jinJavaUtils,configRestClientApi);
    }
}
