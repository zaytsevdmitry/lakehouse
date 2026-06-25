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
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.HashMap;
import java.util.Map;

public class CheckImpl implements Check{
    private final SQLTemplateDTO sqlTemplateDTO;
    private final SparkSession sparkSession;
    private final JinJavaUtils jinJavaUtils;
    public CheckImpl(
            SQLTemplateDTO sqlTemplateDTO,
            SparkSession sparkSession,
            JinJavaUtils jinJavaUtils) {
        this.sqlTemplateDTO = sqlTemplateDTO;
        this.sparkSession = sparkSession;
        this.jinJavaUtils = jinJavaUtils;
    }

    private Dataset<Row> execute(String sql, Map<String, Object> context){
        return sparkSession.sql(jinJavaUtils.render(sql,context));
    }
    @Override
    public Dataset<Row> nullableColumn(ColumnDTO columnDTO) {

        return execute(sqlTemplateDTO.getColumnNonNullCheckIntegrity(), Map.of(SystemVarKeys.COLUMN, columnDTO));
    }

    private Map<String,Object> getConstraintLocalContext(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        Map<String,Object> result = new HashMap<>();
        result.put(SystemVarKeys.CONSTRAINT_NAME, constraint.getKey());
        result.put(SystemVarKeys.CONSTRAINT, ObjectMapping.asMap(constraint.getValue()));
        return result;
    }
    @Override
    public Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getPrimaryKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getForeignKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getUniqueKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getCheckConstraintCheckIntegrity(), getConstraintLocalContext(constraint));
    }
}
