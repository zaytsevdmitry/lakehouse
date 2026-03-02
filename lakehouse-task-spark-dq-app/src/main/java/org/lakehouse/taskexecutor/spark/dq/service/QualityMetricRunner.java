package org.lakehouse.taskexecutor.spark.dq.service;

import org.apache.spark.sql.*;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.lakehouse.client.api.dto.dq.MetricDQValueDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.spark.dq.runner.TestSetRunner;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQTestSetProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQValueProducerService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
public class QualityMetricRunner {
    private final Long metricId;
    private final SourceConfDTO sourceConfDTO;
    private final ScheduledTaskDTO scheduledTaskDTO;
    private final QualityMetricsConfDTO qualityMetricsConfDTO;
    private final JinJavaUtils jinJavaUtils;
    private final ConfigurableApplicationContext applicationContext;
    private final MetricDQProducerService MetricDQProducerService;
    private final MetricDQTestSetProducerService metricDQTestSetProducerService;
    private final MetricDQValueProducerService metricDQValueProducerService;
    private final SparkSession sparkSession;;
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
        saveMetric(Status.DQMetric.RUNNING);

        for (Map.Entry<String, QualityMetricsConfTestSetDTO> testSet : qualityMetricsConfDTO.getTestSets().entrySet()) {
            createTempView(testSet);
        }

        if (qualityMetricsConfDTO.getMetric() != null){
            // for thresholds
            createTempView(Map.entry(qualityMetricsConfDTO.getKeyName(),qualityMetricsConfDTO.getMetric()));
            // for save metrics
            sparkSession
                    .table(qualityMetricsConfDTO.getKeyName())
                    .withColumn("metricId",functions.lit(metricId))
                    .as(Encoders.bean(MetricDQValueDTO.class))
                    .collectAsList()
                    .forEach(metricDQValueProducerService::send);
        }

        // perform threshold violation
        List<String> violated = new ArrayList<>();
        for (Map.Entry<String, QualityMetricsConfTestSetDTO> testSet : qualityMetricsConfDTO.getThresholds().entrySet()) {
            if (createDataset(testSet).count() > 0) {
                violated.add(testSet.getKey());
            }
        }
        if (violated.isEmpty()) {
            saveMetric(Status.DQMetric.SUCCESS);
        }
        else {
            saveMetric(Status.DQMetric.FAILED);
            if (qualityMetricsConfDTO.getDqThresholdViolationLevel().equals(Types.DQThresholdViolationLevel.error))
                throw new TaskFailedException("");
        }
    }


    private Dataset<Row> createDataset(Map.Entry<String, QualityMetricsConfTestSetDTO> testSet ) throws TaskFailedException, TaskConfigurationException {
        TestSetRunner testSetRunner = getTestSetRunner(testSet.getValue().getType());
        Dataset<Row> testSetDataSet = testSetRunner.run(testSet, sourceConfDTO, scheduledTaskDTO, jinJavaUtils);
        //todo remove/ it's debug
        testSetDataSet.show();
        return testSetDataSet;
    }
    private void createTempView(Map.Entry<String, QualityMetricsConfTestSetDTO> testSet ) throws TaskFailedException, TaskConfigurationException {
        createDataset(testSet).createOrReplaceTempView(testSet.getKey());

    }
    private TestSetRunner getTestSetRunner(Types.DQMetricTestSetType type) throws TaskFailedException {
        return switch (type) {
            case integrity -> applicationContext.getBean(ConstraintTestSetRunner.class);
            case sparkSQL   -> applicationContext.getBean(SparkSQLTestSetRunner.class);
            default         -> throw new TaskFailedException("Unknown DQMetricsType");
        };
    }

    private void saveMetric(Status.DQMetric status){
        MetricDQStatusDTO metric = new MetricDQStatusDTO(
                metricId,
                sourceConfDTO.getTargetDataSource().getKeyName(),
                sourceConfDTO.getTargetDataSet().getDatabaseSchemaName(),
                sourceConfDTO.getTargetDataSet().getTableName(),
                DateTimeUtils.now(),
                DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskDTO.getTargetDateTime()),
                DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskDTO.getIntervalStartDateTime()),
                DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskDTO.getIntervalEndDateTime()),
                status,
                qualityMetricsConfDTO.getKeyName()
        );
        MetricDQProducerService.send(metric);
    }
}
