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

package org.jarina.taskexecutor.spark.dq.test;

import tools.jackson.core.JacksonException;
import org.apache.spark.sql.SparkSession;
import org.jarina.client.api.dto.common.SQLTemplateDTO;
import org.jarina.client.api.dto.configs.dataset.DataSetDTO;
import org.jarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.jarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.jarina.client.api.dto.task.SourceConfDTO;
import org.jarina.client.api.exception.TaskConfigurationException;
import org.jarina.client.rest.config.ConfigRestClientApi;
import org.jarina.jinja.java.JinJavaFactory;
import org.jarina.jinja.java.JinJavaUtils;
import org.jarina.task.executor.spark.api.service.CatalogActivatorService;
import org.jarina.taskexecutor.api.datasource.DataSourceManipulator;
import org.jarina.taskexecutor.api.factory.SQLTemplateFactory;
import org.jarina.taskexecutor.spark.dataset.datasourcemanipulator.SparkDataSourceManipulatorFactory;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            SQLTemplateFactory sqlTemplateFactory,
            ScheduledTaskDTO scheduledTaskDTO,
            SparkSession sparkSession,
            String dataSetKeyName,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException, JacksonException {

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils(sourceConfDTO,scheduledTaskDTO);

        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession);
        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        SQLTemplateDTO sqlTemplateDTO = sqlTemplateFactory.mergeSqlTemplate(scheduledTaskDTO);
        new CatalogActivatorService(
                sparkSession)
                .activate(sourceConfDTO);
        return manipulatorFactory.buildDataSourceManipulator(dataSourceDTO, dataSetDTO,sqlTemplateDTO,jinJavaUtils,configRestClientApi);
    }

   /*     public static DataSourceManipulator getSparkSQLDataSourceManipulatorPg(
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
    }*/
}
