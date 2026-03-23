package org.lakehouse.taskexecutor.spark.dq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.factory.ConstructFactory;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.spark.dq.runner.TestSetRunner;
import org.lakehouse.taskexecutor.spark.dq.runner.integrity.Check;
import org.lakehouse.taskexecutor.spark.dq.runner.integrity.CheckImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConstraintTestSetRunner implements TestSetRunner {
    private final  Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SparkSession sparkSession;

    public ConstraintTestSetRunner(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @Override
    public Dataset<Row> run(
            Map.Entry<String, QualityMetricsConfTestSetDTO> qualityMetricsConfTestSetDTO,
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils
            ) throws TaskConfigurationException, JsonProcessingException {

        Check check = prepareCheck(sourceConfDTO, jinJavaUtils);

        Map<String, DataSetConstraintDTO>  constraints = ConstructFactory.constraintsEnabled(sourceConfDTO.getTargetDataSet());

        Dataset<Row> result = sparkSession.emptyDataFrame();

        for (Map.Entry<String, DataSetConstraintDTO> constraint: constraints.entrySet()) {
            Dataset<Row> current = switch (constraint.getValue().getType()) {
                case primary -> check.getPrimary(constraint);
                case foreign -> check.getForeign(constraint);
                case unique -> check.getUnique(constraint);
                case check -> check.getCheck(constraint);
            };
            result.unionAll(current);
        }
        for (ColumnDTO column: ConstructFactory.nullableColumns(sourceConfDTO.getTargetDataSet())){
            result.unionAll(check.nullableColumn(column));
        }
        return result;
    }

    private Check prepareCheck(SourceConfDTO sourceConfDTO, JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        return new CheckImpl(
                SQLTemplateFactory.mergeSqlTemplate(
                        sourceConfDTO.getTargetDriver(),
                        sourceConfDTO.getTargetDataSource(),
                        sourceConfDTO.getTargetDataSet()),
                sparkSession,
                jinJavaUtils);
    }


}
