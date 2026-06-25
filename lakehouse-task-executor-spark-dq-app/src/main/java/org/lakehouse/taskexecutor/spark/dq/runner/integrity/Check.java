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

package org.lakehouse.taskexecutor.spark.dq.runner.integrity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;

import java.util.Map;

public interface Check {
    Dataset<Row> nullableColumn(ColumnDTO columnDTO);
    Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
}
