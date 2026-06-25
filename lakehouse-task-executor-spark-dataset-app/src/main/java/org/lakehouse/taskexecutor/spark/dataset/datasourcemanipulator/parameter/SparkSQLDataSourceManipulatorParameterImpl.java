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

package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;

public record SparkSQLDataSourceManipulatorParameterImpl(
        SparkSession sparkSession,
        SparkExecuteUtils executeUtils,
        SQLTemplateResolver sqlTemplateResolver,
        DataSetDTO dataSetDTO)
        implements SparkSQLDataSourceManipulatorParameter {
}
