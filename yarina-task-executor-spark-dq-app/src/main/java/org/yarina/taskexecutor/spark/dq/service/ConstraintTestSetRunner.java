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

import tools.jackson.core.JacksonException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.yarina.client.api.constant.Types;
import org.yarina.client.api.dto.configs.dataset.ColumnDTO;
import org.yarina.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.yarina.client.api.dto.configs.dataset.DataSetDTO;
import org.yarina.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.yarina.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.yarina.client.api.dto.configs.schedule.TaskDTO;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.factory.ConstructFactory;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.taskexecutor.api.factory.SQLTemplateFactory;
import org.yarina.taskexecutor.api.facade.SQLTemplateResolver;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.taskexecutor.spark.dq.runner.TestSetRunner;
import org.yarina.taskexecutor.spark.dq.runner.integrity.Check;
import org.yarina.taskexecutor.spark.dq.runner.integrity.CheckImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConstraintTestSetRunner implements TestSetRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final StructType INTEGRITY_RESULT_SCHEMA = new StructType()
            .add("subMetricName", DataTypes.StringType, false)
            .add("value", DataTypes.createDecimalType(38, 18), false)
            .add("severity", DataTypes.StringType, false);

    private final SparkSession sparkSession;
    private final SQLTemplateFactory sqlTemplateFactory;
    private final ConfigRestClientApi configRestClientApi;

    public ConstraintTestSetRunner(
            SparkSession sparkSession,
            SQLTemplateFactory sqlTemplateFactory,
            ConfigRestClientApi configRestClientApi) {
        this.sparkSession = sparkSession;
        this.sqlTemplateFactory = sqlTemplateFactory;
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public Dataset<Row> run(
            Map.Entry<String, QualityMetricsConfTestSetDTO> qualityMetricsConfTestSetDTO,
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            String qualityMetricsConfKeyName,
            JinJavaUtils jinJavaUtils
            ) throws TaskConfigurationException, JacksonException {

        ensureReferenceDataSets(sourceConfDTO);
        Check check = prepareCheck(scheduledTaskDTO, jinJavaUtils, sourceConfDTO);

        Map<String, DataSetConstraintDTO> constraints = ConstructFactory.constraintsEnabled(sourceConfDTO.getTargetDataSet());

        List<Dataset<Row>> results = new ArrayList<>();
        for (Map.Entry<String, DataSetConstraintDTO> constraint: constraints.entrySet()) {
            Dataset<Row> current = switch (constraint.getValue().getType()) {
                case PRIMARY -> check.getPrimary(constraint);
                case FOREIGN -> check.getForeign(constraint);
                case UNIQUE -> check.getUnique(constraint);
                case CHECK -> check.getCheck(constraint);
            };
            results.add(normalize(
                    current,
                    constraint.getKey(),
                    severity(constraint.getValue().getType()),
                    "constraint " + constraint.getKey()));
        }
        for (ColumnDTO column: ConstructFactory.nullableColumns(sourceConfDTO.getTargetDataSet())){
            results.add(normalize(
                    check.nullableColumn(column),
                    column.getName(),
                    Types.DQThresholdViolationLevel.ERROR,
                    "column " + column.getName()));
        }

        results.add(normalize(
                check.schemaDrift(),
                "schema_drift",
                Types.DQThresholdViolationLevel.WARN,
                "schema drift"));

        return results.stream()
                .reduce(Dataset::unionByName)
                .orElseGet(() -> sparkSession.createDataFrame(List.of(), INTEGRITY_RESULT_SCHEMA));
    }

    /**
     * Foreign key checks may reference a dataset which is not a direct source of the target dataset.
     * The referenced dataset is needed both for type introspection and for {@code refCat} rendering.
     */
    private void ensureReferenceDataSets(SourceConfDTO sourceConfDTO) {
        DataSetDTO target = sourceConfDTO.getTargetDataSet();
        if (target == null) {
            return;
        }
        Map<String, DataSetDTO> dataSets = sourceConfDTO.getDataSets();
        for (DataSetConstraintDTO constraint : target.getConstraints().values()) {
            ForeignKeyReferenceDTO reference = constraint.getReference();
            if (constraint.getType() != Types.ConstraintType.FOREIGN
                    || reference == null
                    || reference.getDataSetKeyName() == null
                    || dataSets.containsKey(reference.getDataSetKeyName())) {
                continue;
            }
            DataSetDTO referenced = configRestClientApi.getDataSetDTO(reference.getDataSetKeyName());
            if (referenced == null) {
                logger.warn("Referenced dataset {} for constraint {} was not found",
                        reference.getDataSetKeyName(), constraint);
                continue;
            }
            dataSets.put(referenced.getKeyName(), referenced);
        }
    }

    /**
     * Ensures every integrity result has the common {@code subMetricName/value/severity} shape,
     * so that custom database templates may return only a value and still participate in unioning.
     */
    private Dataset<Row> normalize(
            Dataset<Row> dataset,
            String defaultSubMetricName,
            Types.DQThresholdViolationLevel defaultSeverity,
            String source) throws TaskConfigurationException {
        List<String> columns = List.of(dataset.columns());
        if (!columns.contains("value")) {
            throw new TaskConfigurationException(
                    "Integrity " + source + " returned no 'value' column; columns: " + columns);
        }

        org.apache.spark.sql.Column subMetricName = columns.contains("subMetricName")
                ? org.apache.spark.sql.functions.coalesce(
                        org.apache.spark.sql.functions.col("subMetricName"),
                        org.apache.spark.sql.functions.lit(defaultSubMetricName))
                : org.apache.spark.sql.functions.lit(defaultSubMetricName);
        org.apache.spark.sql.Column severity = columns.contains("severity")
                ? org.apache.spark.sql.functions.coalesce(
                        org.apache.spark.sql.functions.col("severity"),
                        org.apache.spark.sql.functions.lit(defaultSeverity.name()))
                : org.apache.spark.sql.functions.lit(defaultSeverity.name());

        return dataset.select(
                subMetricName.cast(DataTypes.StringType).as("subMetricName"),
                org.apache.spark.sql.functions.col("value")
                        .cast(DataTypes.createDecimalType(38, 18)).as("value"),
                org.apache.spark.sql.functions.upper(severity.cast(DataTypes.StringType)).as("severity"));
    }

    private static Types.DQThresholdViolationLevel severity(Types.ConstraintType type) {
        return type == Types.ConstraintType.FOREIGN
                ? Types.DQThresholdViolationLevel.WARN
                : Types.DQThresholdViolationLevel.ERROR;
    }

    private Check prepareCheck(
            TaskDTO taskDTO,
            JinJavaUtils jinJavaUtils,
            SourceConfDTO sourceConfDTO) throws TaskConfigurationException {
        return new CheckImpl(
                new SQLTemplateResolver(
                        configRestClientApi,
                        sqlTemplateFactory.mergeSqlTemplate(taskDTO)),
                sparkSession,
                jinJavaUtils,
                sourceConfDTO);
    }


}
