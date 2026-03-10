package org.lakehouse.taskexecutor.spark.dq.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.spark.dq.runner.TestSetRunner;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SparkSQLTestSetRunner implements TestSetRunner {
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
        String script = qualityMetricsConfTestSetDTO.getValue()
                .getScripts()
                .stream()
                .sorted(Comparator.comparingInt(ScriptReferenceDTO::getOrder))
                .map(sr -> configRestClientApi.getScript(sr.getKey()))
                .collect(Collectors.joining(SystemVarKeys.SCRIPT_DELIMITER));
        String sql = jinJavaUtils.render(script);
        sparkSession.log().info("Query is: {}",sql);
        Dataset<Row> result = sparkSession.sql(sql);
        result.createOrReplaceTempView(qualityMetricsConfTestSetDTO.getKey());
        return result;
    }

}
