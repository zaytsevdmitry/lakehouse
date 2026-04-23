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
