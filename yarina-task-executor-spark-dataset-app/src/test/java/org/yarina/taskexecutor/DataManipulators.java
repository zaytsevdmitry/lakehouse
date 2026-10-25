/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.taskexecutor;

import tools.jackson.core.JacksonException;
import org.apache.spark.sql.SparkSession;
import org.yarina.client.api.dto.common.SQLTemplateDTO;
import org.yarina.client.api.dto.configs.dataset.DataSetDTO;
import org.yarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.utils.ObjectMapping;
import org.yarina.jinja.java.JinJavaFactory;
import org.yarina.taskexecutor.api.factory.SQLTemplateFactory;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.task.executor.spark.api.service.CatalogActivatorService;
import org.yarina.taskexecutor.api.datasource.DataSourceManipulator;
import org.yarina.taskexecutor.spark.dataset.datasourcemanipulator.SparkDataSourceManipulatorFactory;

import java.util.HashMap;

public class DataManipulators {
    public static DataSourceManipulator getIcebergDataSourceManipulator(
            SQLTemplateFactory sqlTemplateFactory,
            ScheduledTaskDTO scheduledTaskDTO,
            SparkSession sparkSession,
            String dataSetKeyName,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException, JacksonException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils(new HashMap<>(),sourceConfDTO,scheduledTaskDTO);
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
