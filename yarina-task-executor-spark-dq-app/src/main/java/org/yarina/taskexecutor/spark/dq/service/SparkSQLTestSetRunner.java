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

package org.yarina.taskexecutor.spark.dq.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.yarina.client.api.constant.SystemVarKeys;
import org.yarina.client.api.dto.configs.ScriptReferenceDTO;
import org.yarina.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.utils.DateTimeUtils;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.client.rest.exception.ScriptBuildException;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.taskexecutor.spark.dq.runner.TestSetRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
            String qualityMetricsConfKeyName,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        String testSetKey = qualityMetricsConfTestSetDTO.getKey();
        String dataSetKeyName = sourceConfDTO.getTargetDataSetKeyName();
        List<ScriptReferenceDTO> scriptReferences = qualityMetricsConfTestSetDTO.getValue()
                .getScripts();
        logger.info("Taking script from config of {}.{}, script reference count {}",
                qualityMetricsConfKeyName, testSetKey, scriptReferences.size());

        String script;
        try {
            script = configRestClientApi.getScriptByListOfReference(scriptReferences);
        } catch (ScriptBuildException e) {
            throw new TaskConfigurationException(
                    diagnostics(qualityMetricsConfKeyName, testSetKey, dataSetKeyName, e), e);
        }

        String sql;
        try {
            sql = jinJavaUtils.render(script, renderContext(qualityMetricsConfKeyName, scheduledTaskDTO));
        } catch (Exception e) {
            throw new TaskConfigurationException(
                    diagnostics(qualityMetricsConfKeyName, testSetKey, dataSetKeyName, e), e);
        }

        logger.debug("Rendered SQL for QM {}, testSet {}, dataset {}:\n{}",
                qualityMetricsConfKeyName, testSetKey, dataSetKeyName, sql);

        try {
            Dataset<Row> result = sparkSession.sql(sql);
            result.createOrReplaceTempView(testSetKey);
            return result;
        } catch (Exception e) {
            throw new TaskConfigurationException(
                    diagnostics(qualityMetricsConfKeyName, testSetKey, dataSetKeyName, e), e);
        }
    }

    private Map<String, Object> renderContext(String qualityMetricsConfKeyName, ScheduledTaskDTO scheduledTaskDTO) {
        Map<String, Object> context = new HashMap<>();
        context.put(SystemVarKeys.QUALITY_METRICS_CONF_KEY_NAME, qualityMetricsConfKeyName);
        context.put(SystemVarKeys.TARGET_INTERVAL_START_DATE_KEY,
                toDate(scheduledTaskDTO.getIntervalStartDateTime()));
        context.put(SystemVarKeys.TARGET_INTERVAL_END_DATE_KEY,
                toDate(scheduledTaskDTO.getIntervalEndDateTime()));
        return context;
    }

    private static String toDate(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return "";
        }
        try {
            OffsetDateTime parsed = DateTimeUtils.parseDateTimeFormatWithTZ(dateTime);
            return parsed == null ? "" : parsed.toLocalDate().toString();
        } catch (RuntimeException e) {
            return dateTime.length() >= 10 ? dateTime.substring(0, 10) : dateTime;
        }
    }

    private static String diagnostics(
            String qualityMetricsConfKeyName,
            String testSetKey,
            String dataSetKeyName,
            Exception original) {
        String message = original.getMessage();
        if (message == null || message.isBlank()) {
            message = original.getClass().getSimpleName();
        }
        return String.format("QM %s, testSet %s, dataset %s: %s",
                qualityMetricsConfKeyName, testSetKey, dataSetKeyName, message);
    }
}
