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

package org.yarina.taskexecutor.spark.dq.runner.integrity;

import tools.jackson.core.JacksonException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.yarina.client.api.constant.SystemVarKeys;
import org.yarina.client.api.constant.Types;
import org.yarina.client.api.dto.common.SQLTemplateGetter;
import org.yarina.client.api.dto.configs.dataset.ColumnDTO;
import org.yarina.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.yarina.client.api.dto.configs.dataset.DataSetDTO;
import org.yarina.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.utils.ObjectMapping;
import org.yarina.jinja.java.JinJavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckImpl implements Check {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SQLTemplateGetter sqlTemplateGetter;
    private final SparkSession sparkSession;
    private final JinJavaUtils jinJavaUtils;
    private final SourceConfDTO sourceConfDTO;

    public CheckImpl(
            SQLTemplateGetter sqlTemplateGetter,
            SparkSession sparkSession,
            JinJavaUtils jinJavaUtils,
            SourceConfDTO sourceConfDTO) {
        this.sqlTemplateGetter = sqlTemplateGetter;
        this.sparkSession = sparkSession;
        this.jinJavaUtils = jinJavaUtils;
        this.sourceConfDTO = sourceConfDTO;
    }

    @FunctionalInterface
    private interface SqlTemplate {
        String get() throws Exception;
    }

    private Dataset<Row> execute(
            SqlTemplate sqlTemplate,
            Map<String, Object> context,
            String constraintName,
            Types.ConstraintType type,
            String columns,
            String foreignKeyTypeSummary,
            String foreignKeyTypeMismatch) throws TaskConfigurationException, JacksonException {
        try {
            String sql = sqlTemplate.get();
            if (sql == null || sql.isBlank()) {
                throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                        constraintName, type, dataSetKeyName(), columns,
                        "SQL template is not configured for constraint type " + (type == null ? "nullable" : type.label)));
            }
            String rendered = jinJavaUtils.render(sql, context);
            logger.debug("Rendered integrity SQL for constraint {}, type {}:\n{}", constraintName, type, rendered);
            return sparkSession.sql(rendered);
        } catch (TaskConfigurationException e) {
            throw e;
        } catch (Exception e) {
            String message = IntegrityDiagnostics.constraintMessage(
                    constraintName, type, dataSetKeyName(), columns, e.getMessage());
            if (foreignKeyTypeMismatch != null && !foreignKeyTypeMismatch.isBlank()) {
                message = foreignKeyTypeMismatch + ". " + message;
            } else if (foreignKeyTypeSummary != null && !foreignKeyTypeSummary.isBlank()) {
                message = message + " [" + foreignKeyTypeSummary + "]";
            }
            throw new TaskConfigurationException(message, e);
        }
    }

    @Override
    public Dataset<Row> nullableColumn(ColumnDTO columnDTO) throws TaskConfigurationException {
        Map<String, Object> context = baseContext();
        context.put(SystemVarKeys.CONSTRAINT_NAME, columnDTO.getName());
        context.put(SystemVarKeys.COLUMN, columnDTO);
        context.put(SystemVarKeys.CONSTRAINT_COLUMNS, List.of(columnDTO.getName()));
        context.put(SystemVarKeys.CONSTRAINT_SEVERITY, Types.DQThresholdViolationLevel.ERROR.name());
        return execute(sqlTemplateGetter::getColumnNonNullCheckIntegrity, context,
                columnDTO.getName(), null, columnDTO.getName(), null, null);
    }

    private Map<String, Object> getConstraintLocalContext(Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException {
        Map<String, Object> result = baseContext();
        result.put(SystemVarKeys.CONSTRAINT_NAME, constraint.getKey());
        result.put(SystemVarKeys.CONSTRAINT, ObjectMapping.asMap(constraint.getValue()));
        result.put(SystemVarKeys.CONSTRAINT_COLUMNS, splitColumns(constraint.getValue().getColumns()));
        result.put(SystemVarKeys.CONSTRAINT_SEVERITY, severity(constraint.getValue().getType()).name());
        return result;
    }

    private Map<String, Object> baseContext() {
        Map<String, Object> result = new HashMap<>();
        result.put(SystemVarKeys.TARGET_DATASET_KEY_NAME, dataSetKeyName());
        return result;
    }

    @Override
    public Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException, TaskConfigurationException {
        return executeConstraint(sqlTemplateGetter::getPrimaryKeyCheckIntegrity, constraint);
    }

    @Override
    public Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException, TaskConfigurationException {
        Map<String, Object> context = getConstraintLocalContext(constraint);
        ForeignKeyMetadata metadata = foreignKeyMetadata(constraint, context);
        return execute(sqlTemplateGetter::getForeignKeyCheckIntegrity, context,
                constraint.getKey(), constraint.getValue().getType(), constraint.getValue().getColumns(),
                metadata.typeSummary(), metadata.typeMismatch());
    }

    @Override
    public Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException, TaskConfigurationException {
        return executeConstraint(sqlTemplateGetter::getUniqueKeyCheckIntegrity, constraint);
    }

    @Override
    public Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException, TaskConfigurationException {
        return executeConstraint(sqlTemplateGetter::getCheckConstraintCheckIntegrity, constraint);
    }

    private Dataset<Row> executeConstraint(SqlTemplate sqlTemplate, Map.Entry<String, DataSetConstraintDTO> constraint)
            throws JacksonException, TaskConfigurationException {
        return execute(sqlTemplate, getConstraintLocalContext(constraint),
                constraint.getKey(), constraint.getValue().getType(), constraint.getValue().getColumns(),
                null, null);
    }

    /**
     * Compares the declared columnSchema with the actual schema of the target table.
     * Returns a single {@code schema_drift} row with the number of missing columns.
     */
    @Override
    public Dataset<Row> schemaDrift() throws TaskConfigurationException {
        String constraintName = "schema_drift";
        String columns = expectedColumnNames().stream().collect(Collectors.joining(","));
        try {
            String tableReference = jinJavaUtils.render("{{ refCat('" + dataSetKeyName() + "') }}").trim();
            if (tableReference.isBlank()) {
                throw new IllegalStateException("target table reference is empty");
            }
            Set<String> actualColumns = Arrays.stream(sparkSession.table(tableReference).columns())
                    .map(column -> column.toLowerCase(java.util.Locale.ROOT))
                    .collect(Collectors.toSet());
            long missing = expectedColumnNames()
                    .stream()
                    .map(column -> column.toLowerCase(java.util.Locale.ROOT))
                    .filter(column -> !actualColumns.contains(column))
                    .count();

            StructType schema = new StructType()
                    .add("subMetricName", DataTypes.StringType, false)
                    .add("value", DataTypes.LongType, false)
                    .add("severity", DataTypes.StringType, false);
            List<Row> rows = List.of(RowFactory.create(
                    constraintName, missing, Types.DQThresholdViolationLevel.WARN.name()));
            return sparkSession.createDataFrame(rows, schema);
        } catch (Exception e) {
            throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                    constraintName, Types.ConstraintType.CHECK, dataSetKeyName(), columns, e.getMessage()), e);
        }
    }

    private ForeignKeyMetadata foreignKeyMetadata(
            Map.Entry<String, DataSetConstraintDTO> constraint,
            Map<String, Object> context) throws TaskConfigurationException {
        ForeignKeyReferenceDTO reference = constraint.getValue().getReference();
        if (reference == null || reference.getDataSetKeyName() == null) {
            throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                    constraint.getKey(), constraint.getValue().getType(), dataSetKeyName(),
                    constraint.getValue().getColumns(), "foreign key reference is not configured"));
        }

        String referenceDataSetKeyName = reference.getDataSetKeyName();
        DataSetDTO referenceDataSet = sourceConfDTO.getDataSets().get(referenceDataSetKeyName);
        if (referenceDataSet == null) {
            throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                    constraint.getKey(), constraint.getValue().getType(), dataSetKeyName(),
                    constraint.getValue().getColumns(),
                    "reference dataset " + referenceDataSetKeyName + " is not available in the run context"));
        }

        DataSetConstraintDTO referenceConstraint = referenceDataSet.getConstraints()
                .get(reference.getConstraintName());
        if (referenceConstraint == null) {
            throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                    constraint.getKey(), constraint.getValue().getType(), dataSetKeyName(),
                    constraint.getValue().getColumns(),
                    "reference constraint " + reference.getConstraintName()
                            + " is not found in dataset " + referenceDataSetKeyName));
        }

        List<String> sourceColumns = splitColumns(constraint.getValue().getColumns());
        List<String> referenceColumns = splitColumns(referenceConstraint.getColumns());
        if (sourceColumns.size() != referenceColumns.size()) {
            throw new TaskConfigurationException(IntegrityDiagnostics.constraintMessage(
                    constraint.getKey(), constraint.getValue().getType(), dataSetKeyName(),
                    constraint.getValue().getColumns(),
                    "foreign key column count " + sourceColumns.size()
                            + " does not match reference column count " + referenceColumns.size()));
        }

        List<String> sourceTypes = columnTypes(sourceConfDTO.getTargetDataSet(), sourceColumns);
        List<String> referenceTypes = columnTypes(referenceDataSet, referenceColumns);
        List<String> castTypes = new ArrayList<>();
        for (int i = 0; i < sourceColumns.size(); i++) {
            String referenceType = referenceTypes.get(i);
            castTypes.add(referenceType == null || referenceType.isBlank() ? "string" : referenceType);
        }

        String mismatch = IntegrityDiagnostics.fkTypeMismatch(
                constraint.getKey(), referenceDataSetKeyName,
                sourceColumns, sourceTypes, referenceColumns, referenceTypes);

        context.put(SystemVarKeys.FOREIGN_REFERENCE_DATA_SET_KEY_NAME, referenceDataSetKeyName);
        context.put(SystemVarKeys.FOREIGN_REFERENCE_CONSTRAINT_NAME, reference.getConstraintName());
        context.put(SystemVarKeys.FOREIGN_SOURCE_COLUMNS, sourceColumns);
        context.put(SystemVarKeys.FOREIGN_REFERENCE_COLUMNS, referenceColumns);
        context.put(SystemVarKeys.FOREIGN_SOURCE_COLUMN_TYPES, sourceTypes);
        context.put(SystemVarKeys.FOREIGN_REFERENCE_COLUMN_TYPES, referenceTypes);
        context.put(SystemVarKeys.FOREIGN_SOURCE_CAST_TYPES, castTypes);
        context.put(SystemVarKeys.FOREIGN_TYPE_MISMATCH, !mismatch.isBlank());
        context.put(SystemVarKeys.FOREIGN_TYPE_MISMATCH_MESSAGE, mismatch);

        return new ForeignKeyMetadata(typeSummary(constraint.getKey(), referenceDataSetKeyName,
                sourceColumns, sourceTypes, referenceColumns, referenceTypes), mismatch);
    }

    private static String typeSummary(
            String constraintName,
            String referenceDataSetKeyName,
            List<String> sourceColumns,
            List<String> sourceTypes,
            List<String> referenceColumns,
            List<String> referenceTypes) {
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < Math.min(sourceColumns.size(), referenceColumns.size()); i++) {
            pairs.add(String.format("source %s (%s) vs reference %s.%s (%s)",
                    sourceColumns.get(i), sourceTypes.get(i),
                    referenceDataSetKeyName, referenceColumns.get(i), referenceTypes.get(i)));
        }
        return String.format("FK %s column types: %s", constraintName, String.join(", ", pairs));
    }

    private List<String> expectedColumnNames() {
        return sourceConfDTO.getTargetDataSet()
                .getColumnSchema()
                .stream()
                .map(ColumnDTO::getName)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> columnTypes(DataSetDTO dataSetDTO, List<String> columnNames) {
        if (dataSetDTO == null) {
            return columnNames.stream().map(column -> "unknown").toList();
        }
        Map<String, String> types = dataSetDTO.getColumnSchema()
                .stream()
                .filter(column -> column.getName() != null)
                .collect(Collectors.toMap(
                        ColumnDTO::getName,
                        column -> column.getDataType() == null ? "unknown" : column.getDataType(),
                        (left, right) -> left));
        return columnNames.stream().map(column -> types.getOrDefault(column, "unknown")).toList();
    }

    private static List<String> splitColumns(String columns) {
        if (columns == null || columns.isBlank()) {
            return List.of();
        }
        return Arrays.stream(columns.split(","))
                .map(String::trim)
                .filter(column -> !column.isEmpty())
                .toList();
    }

    private static Types.DQThresholdViolationLevel severity(Types.ConstraintType type) {
        return type == Types.ConstraintType.FOREIGN
                ? Types.DQThresholdViolationLevel.WARN
                : Types.DQThresholdViolationLevel.ERROR;
    }

    private String dataSetKeyName() {
        String target = sourceConfDTO.getTargetDataSetKeyName();
        if (target != null && !target.isBlank()) {
            return target;
        }
        DataSetDTO targetDataSet = sourceConfDTO.getTargetDataSet();
        return targetDataSet == null ? "<unknown>" : targetDataSet.getKeyName();
    }

    private record ForeignKeyMetadata(String typeSummary, String typeMismatch) {
    }
}
