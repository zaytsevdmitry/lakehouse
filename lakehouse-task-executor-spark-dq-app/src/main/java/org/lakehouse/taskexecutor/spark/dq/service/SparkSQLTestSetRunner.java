/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.taskexecutor.spark.dq.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.exception.ScriptBuildException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.spark.dq.runner.TestSetRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SparkSQLTestSetRunner implements TestSetRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SparkSession sparkSession;
    private final ConfigRestClientApi configRestClientApi;

    public SparkSQLTestSetRunner(SparkSession sparkSession, ConfigRestClientApi configRestClientApi) {
        this.sparkSession = sparkSession;
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public Dataset<Row> run (
            Map.Entry<String, QualityMetricsConfTestSetDTO> qualityMetricsConfTestSetDTO,
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        String script = null;
        logger.info("Take script from config of {}",qualityMetricsConfTestSetDTO.getKey());
        List<ScriptReferenceDTO> scriptReferences = qualityMetricsConfTestSetDTO.getValue()
                .getScripts();
        logger.info("Script reference count {}", scriptReferences.size());
        logger.info("QMTS [ {} ]", qualityMetricsConfTestSetDTO.toString());
        try {
            script = configRestClientApi.getScriptByListOfReference(scriptReferences);
        } catch (ScriptBuildException e) {
            throw new TaskConfigurationException(e);
        }

        sparkSession.log().info("Script template  is: [ {} ]", script);
        String sql = jinJavaUtils.render(script);
        sparkSession.log().info("Query is:[ {} ]",sql);
        Dataset<Row> result = sparkSession.sql(sql);
        result.createOrReplaceTempView(qualityMetricsConfTestSetDTO.getKey());
        return result;
    }

}
