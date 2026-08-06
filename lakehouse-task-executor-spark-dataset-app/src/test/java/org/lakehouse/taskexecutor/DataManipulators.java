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

package org.lakehouse.taskexecutor;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.api.factory.SQLTemplateFactory;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.SparkDataSourceManipulatorFactory;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            SQLTemplateFactory sqlTemplateFactory,
            ScheduledTaskDTO scheduledTaskDTO,
            SparkSession sparkSession,
            String dataSetKeyName,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException, JsonProcessingException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils(sourceConfDTO,scheduledTaskDTO);
        SparkDataSourceManipulatorFactory manipulatorFactory =
                new SparkDataSourceManipulatorFactory(sparkSession);
        DataSetDTO dataSetDTO = sourceConfDTO.getDataSets().get(dataSetKeyName);
        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
        SQLTemplateDTO sqlTemplateDTO = sqlTemplateFactory.mergeSqlTemplate(scheduledTaskDTO);
        /*new CatalogActivatorService(
                sparkSession)
                .activate(sourceConfDTO);*/
        return manipulatorFactory.buildDataSourceManipulator(dataSourceDTO, dataSetDTO,sqlTemplateDTO,jinJavaUtils,configRestClientApi);
    }
}
