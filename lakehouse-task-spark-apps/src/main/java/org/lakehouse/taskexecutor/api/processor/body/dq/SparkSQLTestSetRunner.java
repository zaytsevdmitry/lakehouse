package org.lakehouse.taskexecutor.api.processor.body.dq;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SparkSQLTestSetRunner implements TestSetRunner{
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
            ExecuteUtils executeUtils, JinJavaUtils jinJavaUtils) {
        String script = qualityMetricsConfTestSetDTO.getValue()
                .getScripts()
                .stream()
                .sorted(Comparator.comparingInt(ScriptReferenceDTO::getOrder))
                .map(sr -> configRestClientApi.getScript(sr.getKey()))
                .collect(Collectors.joining(SystemVarKeys.SCRIPT_DELIMITER));


        return sparkSession.sql(jinJavaUtils.render(script));
    }

}
