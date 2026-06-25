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

package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtilsAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SparkExecuteUtilsImpl
        extends ExecuteUtilsAbstract
        implements SparkExecuteUtils{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SparkSession sparkSession;
    public SparkExecuteUtilsImpl(
            JinJavaUtils jinJavaUtils,
            DataSourceDTO dataSourceDTO,
            DriverDTO driverDTO,
            SparkSession sparkSession) {
        super(jinJavaUtils, dataSourceDTO, driverDTO);
        this.sparkSession = sparkSession;
    }

    @Override
    public void execute(String sql, Map<String,Object> localContext) throws ExecuteException {
        executeQuery(sql,localContext);
    }

    @Override
    public void execute(String sql) throws ExecuteException {
        execute(sql, new HashMap<>());
    }

    private Object executeGetResultObject(String sql, Map<String,Object> localContext) throws ExecuteException {

        List<Row> resultList = executeQuery(sql,localContext)
                .select(RESULT_COLUMN_NAME)
                .collectAsList();

        if (resultList.size() != 1) {
            throw new ExecuteException(String.format("Expected only one row, but %d entries were found", resultList.size()));
        }

        return resultList.get(0).get(0);
    }
    @Override
    public Integer executeGetResultInt(String sql, Map<String,Object> localContext) throws ExecuteException {
         return (Integer) executeGetResultObject(sql,localContext);
    }
    @Override
    public Long executeGetResultLong(String sql, Map<String,Object> localContext) throws ExecuteException {
        return (Long) executeGetResultObject(sql,localContext);
    }


    public Dataset<Row> executeQuery(String sql)throws ExecuteException {
        return executeQuery(sql, new HashMap<>());
    }


    public Dataset<Row> executeQuery(String sql, Map<String, Object> localContext) throws ExecuteException {

        logger.info("Render query  {}", sql);
        String renderedSQL = getjinJavaUtils().render(sql,localContext);

        logger.info("Execute query {}", renderedSQL);
        return sparkSession.sql(renderedSQL);
    }
}
