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
import org.yarina.client.api.constant.Status;
import org.yarina.client.api.constant.Types;
import org.yarina.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.yarina.client.api.dto.configs.dq.QualityMetricsConfExpectationDTO;
import org.yarina.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.yarina.client.api.dto.configs.dq.QualityMetricsThresholdDTO;
import org.yarina.client.api.dto.dq.MetricDQStatusDTO;
import org.yarina.client.api.dto.dq.MetricDQValueDTO;
import org.yarina.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.yarina.client.api.dto.task.SourceConfDTO;
import org.yarina.client.api.exception.TaskConfigurationException;
import org.yarina.client.api.exception.TaskFailedException;
import org.yarina.client.api.utils.DateTimeUtils;
import org.yarina.jinja.java.JinJavaUtils;
import org.yarina.taskexecutor.spark.dq.runner.TestSetRunner;
import org.yarina.taskexecutor.spark.dq.service.producer.MetricDQProducerService;
import org.yarina.taskexecutor.spark.dq.service.producer.MetricDQTestSetProducerService;
import org.yarina.taskexecutor.spark.dq.service.producer.MetricDQValueProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Service
public class QualityMetricRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Long metricId;
    private final SourceConfDTO sourceConfDTO;
    private final ScheduledTaskDTO scheduledTaskDTO;
    private final QualityMetricsConfDTO qualityMetricsConfDTO;
    private final JinJavaUtils jinJavaUtils;
    private final ConfigurableApplicationContext applicationContext;
    private final MetricDQProducerService MetricDQProducerService;
    private final MetricDQTestSetProducerService metricDQTestSetProducerService;
    private final MetricDQValueProducerService metricDQValueProducerService;
    private final SparkSession sparkSession;

    public QualityMetricRunner(
            Long metricId,
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            QualityMetricsConfDTO qualityMetricsConfDTO,
            JinJavaUtils jinJavaUtils,
            ConfigurableApplicationContext applicationContext,
            MetricDQProducerService MetricDQProducerService,
            MetricDQTestSetProducerService metricDQTestSetProducerService,
            MetricDQValueProducerService metricDQValueProducerService,
            SparkSession sparkSession) {
        this.metricId = metricId;
        this.sourceConfDTO = sourceConfDTO;
        this.scheduledTaskDTO = scheduledTaskDTO;
        this.qualityMetricsConfDTO = qualityMetricsConfDTO;
        this.jinJavaUtils = jinJavaUtils;
        this.applicationContext = applicationContext;
        this.MetricDQProducerService = MetricDQProducerService;
        this.metricDQTestSetProducerService = metricDQTestSetProducerService;
        this.metricDQValueProducerService = metricDQValueProducerService;
        this.sparkSession = sparkSession;
    }

    public void run() throws TaskFailedException, TaskConfigurationException {
        String qmKey = qualityMetricsConfDTO.getKeyName();
        logger.info("QualityMetric {} ran", qmKey);
        saveMetric(Status.DQMetric.RUNNING);

        Map<String, BigDecimal> baselines = new HashMap<>();

        // Metrics and test sets are evaluated before thresholds, so relative thresholds can use
        // values measured in the same QM run regardless of declaration order.
        for (Map.Entry<String, QualityMetricsConfTestSetDTO> testSet : qualityMetricsConfDTO.getTestSets().entrySet()) {
            logger.info("Trying to create test set view {}.{}", qmKey, testSet.getKey());
            Dataset<Row> testSetDataset = createTempView(qmKey, testSet);
            recordBaselines(baselines, testSet.getKey(), testSetDataset);
        }

        if (qualityMetricsConfDTO.getMetric() != null) {
            logger.info("Trying to create test set view {}.metric", qmKey);
            Dataset<Row> metricDataset = createTempView(qmKey, Map.entry(qmKey, qualityMetricsConfDTO.getMetric()));
            recordBaselines(baselines, qmKey, metricDataset);
            if (qualityMetricsConfDTO.isSave()) {
                saveValues(qmKey, "metric", metricDataset);
            }
        }

        logger.info("Trying to run thresholds {}", qmKey);
        List<ThresholdOutcome> outcomes = new ArrayList<>();
        for (Map.Entry<String, QualityMetricsConfTestSetDTO> threshold : qualityMetricsConfDTO.getThresholds().entrySet()) {
            logger.info("Trying to run threshold {}.{}", qmKey, threshold.getKey());
            Dataset<Row> thresholdDataset = createDataset(qmKey, threshold, "threshold");
            ThresholdOutcome outcome = evaluateThreshold(qmKey, threshold, thresholdDataset, baselines);
            outcomes.add(outcome);
            if (outcome.violated()) {
                logger.warn("Threshold {}.{} violated: severity={}, measured={}, description={}",
                        qmKey, outcome.key(), outcome.severity(), outcome.measured(), outcome.description());
            }
            if (qualityMetricsConfDTO.isSave() && outcome.saveValue() != null) {
                saveValue(outcome.key(), outcome.saveValue());
            }
        }

        List<ThresholdOutcome> violations = outcomes.stream()
                .filter(ThresholdOutcome::violated)
                .toList();

        if (violations.isEmpty()) {
            logger.info("{}.metric thresholds passed", qmKey);
            saveMetric(Status.DQMetric.SUCCESS);
            return;
        }

        logger.warn("{}.metric thresholds violated: {}", qmKey, violations.stream()
                .map(outcome -> outcome.key() + " (" + outcome.severity() + ")")
                .collect(Collectors.joining(", ")));
        saveMetric(Status.DQMetric.FAILED);

        List<ThresholdOutcome> errors = violations.stream()
                .filter(outcome -> ThresholdEvaluator.isError(outcome.severity()))
                .toList();
        if (!errors.isEmpty()) {
            throw new TaskFailedException(String.format(
                    "QM %s thresholds violated with ERROR: %s",
                    qmKey,
                    errors.stream()
                            .map(outcome -> outcome.key() + " (" + outcome.description() + ")")
                            .collect(Collectors.joining(", "))));
        }
    }

    private ThresholdOutcome evaluateThreshold(
            String qmKey,
            Map.Entry<String, QualityMetricsConfTestSetDTO> threshold,
            Dataset<Row> dataset,
            Map<String, BigDecimal> baselines) throws TaskConfigurationException {
        String thresholdKey = threshold.getKey();
        QualityMetricsConfExpectationDTO expectation = threshold.getValue().getExpectation();
        Types.DQThresholdViolationLevel qmSeverity = qualityMetricsConfDTO.getDqThresholdViolationLevel();

        if (expectation == null) {
            long count = dataset.count();
            boolean violated = count > 0;
            Types.DQThresholdViolationLevel severity = violated
                    ? thresholdSeverity(dataset, qmSeverity)
                    : qmSeverity;
            String description = String.format("legacy count() > 0 predicate, rows=%d", count);
            return new ThresholdOutcome(thresholdKey, violated, severity, null, null, description);
        }

        String valueColumn = expectation.getValueColumn() == null || expectation.getValueColumn().isBlank()
                ? "value"
                : expectation.getValueColumn();
        if (!List.of(dataset.columns()).contains(valueColumn)) {
            throw new TaskConfigurationException(String.format(
                    "QM %s, threshold %s, dataset %s: valueColumn '%s' is not present in the result. Available columns: %s",
                    qmKey, thresholdKey, sourceConfDTO.getTargetDataSetKeyName(),
                    valueColumn, List.of(dataset.columns())));
        }

        List<Row> rows = dataset.select(valueColumn).takeAsList(2);
        BigDecimal measured = rows.isEmpty() ? null : ThresholdEvaluator.toBigDecimal(rows.get(0).get(0));
        BigDecimal expected = resolveThreshold(qmKey, thresholdKey, expectation.getThreshold(), baselines);

        boolean matches;
        try {
            matches = ThresholdEvaluator.matches(measured, expectation.getOperator(), expected);
        } catch (IllegalArgumentException e) {
            throw new TaskConfigurationException(String.format(
                    "QM %s, threshold %s, dataset %s: %s",
                    qmKey, thresholdKey, sourceConfDTO.getTargetDataSetKeyName(), e.getMessage()), e);
        }

        Types.DQThresholdViolationLevel severity = ThresholdEvaluator.severity(
                expectation.getSeverity(), qmSeverity);
        String description = String.format("measured=%s, operator=%s, threshold=%s",
                measured, expectation.getOperator(), expected);

        BigDecimal saveValue = null;
        if (qualityMetricsConfDTO.isSave() && measured != null) {
            saveValue = measured;
            logger.debug("Threshold {} measured value {} will be saved", thresholdKey, measured);
        }

        return new ThresholdOutcome(thresholdKey, !matches, severity, measured, saveValue, description);
    }

    private BigDecimal resolveThreshold(
            String qmKey,
            String thresholdKey,
            QualityMetricsThresholdDTO threshold,
            Map<String, BigDecimal> baselines) throws TaskConfigurationException {
        if (threshold == null) {
            throw new TaskConfigurationException(String.format(
                    "QM %s, threshold %s, dataset %s: expectation.threshold is not defined",
                    qmKey, thresholdKey, sourceConfDTO.getTargetDataSetKeyName()));
        }
        if (threshold.isRelative()) {
            BigDecimal baseline = baselines.get(threshold.getRelativeTo());
            if (baseline == null) {
                throw new TaskConfigurationException(String.format(
                        "QM %s, threshold %s, dataset %s: relative threshold baseline '%s' is not available in the current run",
                        qmKey, thresholdKey, sourceConfDTO.getTargetDataSetKeyName(), threshold.getRelativeTo()));
            }
            return ThresholdEvaluator.relativeThreshold(baseline, threshold.getFactor());
        }
        if (threshold.getValue() == null) {
            throw new TaskConfigurationException(String.format(
                    "QM %s, threshold %s, dataset %s: expectation.threshold must be a number or {relativeTo, factor}",
                    qmKey, thresholdKey, sourceConfDTO.getTargetDataSetKeyName()));
        }
        return threshold.getValue();
    }

    /**
     * Integrity checks return a row-level severity. If several rows are present, ERROR wins over WARN.
     * Legacy test sets without a severity column fall back to the QM level.
     */
    private Types.DQThresholdViolationLevel thresholdSeverity(
            Dataset<Row> dataset,
            Types.DQThresholdViolationLevel fallback) {
        if (!List.of(dataset.columns()).contains("severity")) {
            return fallback;
        }
        List<Row> severityRows = dataset.select("severity").distinct().collectAsList();
        Types.DQThresholdViolationLevel result = null;
        for (Row row : severityRows) {
            Types.DQThresholdViolationLevel severity = ThresholdEvaluator.parseSeverity(
                    row.isNullAt(0) ? null : row.get(0).toString(), fallback);
            if (severity == Types.DQThresholdViolationLevel.ERROR) {
                return Types.DQThresholdViolationLevel.ERROR;
            }
            if (severity == Types.DQThresholdViolationLevel.WARN || result == null) {
                result = severity;
            }
        }
        return result == null ? fallback : result;
    }

    private void recordBaselines(Map<String, BigDecimal> baselines, String key, Dataset<Row> dataset) {
        List<String> columns = List.of(dataset.columns());
        if (!columns.contains("value")) {
            return;
        }
        List<Row> rows;
        if (columns.contains("subMetricName")) {
            rows = dataset.select("subMetricName", "value").takeAsList(100);
        } else {
            rows = dataset.select("value").takeAsList(100);
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            BigDecimal value;
            String subMetricName = null;
            if (columns.contains("subMetricName")) {
                subMetricName = row.isNullAt(0) ? null : row.get(0).toString();
                value = ThresholdEvaluator.toBigDecimal(row.get(1));
            } else {
                value = ThresholdEvaluator.toBigDecimal(row.get(0));
            }
            if (value == null) {
                continue;
            }
            if (subMetricName != null && !subMetricName.isBlank()) {
                baselines.put(key + "." + subMetricName, value);
            }
            baselines.putIfAbsent(key, value);
        }
    }

    private void saveValues(String qmKey, String prefix, Dataset<Row> dataset) {
        List<String> columns = List.of(dataset.columns());
        if (!columns.contains("value")) {
            logger.warn("Metric {}.{} has no 'value' column and will not be saved as a metric value", qmKey, prefix);
            return;
        }
        List<Row> rows = columns.contains("subMetricName")
                ? dataset.select("subMetricName", "value").collectAsList()
                : dataset.select("value").collectAsList();
        for (Row row : rows) {
            String subMetricName;
            BigDecimal value;
            if (columns.contains("subMetricName")) {
                String measuredName = row.isNullAt(0) ? null : row.get(0).toString();
                subMetricName = measuredName == null || measuredName.isBlank() ? prefix : measuredName;
                value = ThresholdEvaluator.toBigDecimal(row.get(1));
            } else {
                subMetricName = prefix;
                value = ThresholdEvaluator.toBigDecimal(row.get(0));
            }
            if (value != null) {
                saveValue(subMetricName, value);
            }
        }
    }

    private void saveValue(String subMetricName, BigDecimal value) {
        metricDQValueProducerService.send(new MetricDQValueDTO(metricId, subMetricName, value));
    }

    private Dataset<Row> createDataset(
            String qmKey,
            Map.Entry<String, QualityMetricsConfTestSetDTO> testSet,
            String element) throws TaskFailedException, TaskConfigurationException {
        logger.info("Creating {} dataset {}", element, testSet.getKey());
        TestSetRunner testSetRunner = getTestSetRunner(testSet.getValue().getType());
        try {
            return testSetRunner.run(testSet, sourceConfDTO, scheduledTaskDTO, qmKey, jinJavaUtils);
        } catch (TaskConfigurationException e) {
            throw e;
        } catch (JacksonException e) {
            throw new TaskConfigurationException(diagnostics(qmKey, testSet.getKey(), e), e);
        } catch (RuntimeException e) {
            throw new TaskConfigurationException(diagnostics(qmKey, testSet.getKey(), e), e);
        }
    }

    private Dataset<Row> createTempView(
            String qmKey,
            Map.Entry<String, QualityMetricsConfTestSetDTO> testSet) throws TaskFailedException, TaskConfigurationException {
        Dataset<Row> dataset = createDataset(qmKey, testSet, "test set");
        dataset.createOrReplaceTempView(testSet.getKey());
        logger.info("Created view {}", testSet.getKey());
        return dataset;
    }

    private TestSetRunner getTestSetRunner(Types.DQMetricTestSetType type) throws TaskFailedException {
        return switch (type) {
            case INTEGRITY ->  applicationContext.getBean(ConstraintTestSetRunner.class);
            case SPARK_SQL ->  applicationContext.getBean(SparkSQLTestSetRunner.class);
            default         -> throw new TaskFailedException("Unknown DQMetricsType");
        };
    }

    private String diagnostics(String qmKey, String testSetKey, Exception e) {
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : e.getMessage();
        return String.format("QM %s, testSet %s, dataset %s: %s",
                qmKey, testSetKey, sourceConfDTO.getTargetDataSetKeyName(), message);
    }

    private void saveMetric(Status.DQMetric status){
        logger.info("Saving metric status {}: {}", qualityMetricsConfDTO.getKeyName(), status.label);
        MetricDQStatusDTO metric = new MetricDQStatusDTO(
                metricId,
                sourceConfDTO.getTargetDataSource().getKeyName(),
                sourceConfDTO.getTargetDataSet().getDatabaseSchemaName(),
                sourceConfDTO.getTargetDataSet().getTableName(),
                DateTimeUtils.nowStr(),
                scheduledTaskDTO.getTargetDateTime(),
                scheduledTaskDTO.getIntervalStartDateTime(),
                scheduledTaskDTO.getIntervalEndDateTime(),
                status,
                qualityMetricsConfDTO.getKeyName()
        );
        MetricDQProducerService.send(metric);
        logger.info("Metric status sent");
    }

    private record ThresholdOutcome(
            String key,
            boolean violated,
            Types.DQThresholdViolationLevel severity,
            BigDecimal measured,
            BigDecimal saveValue,
            String description) {
    }
}
