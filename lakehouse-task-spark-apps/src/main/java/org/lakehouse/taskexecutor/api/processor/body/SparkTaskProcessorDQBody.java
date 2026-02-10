package org.lakehouse.taskexecutor.api.processor.body;


import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.processor.body.dq.ConstraintTestSetRunner;
import org.lakehouse.taskexecutor.api.processor.body.dq.TestSetRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SparkTaskProcessorDQBody  {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SparkSession sparkSession;
    private final ConfigRestClientApi configRestClientApi;
    private final ConfigurableApplicationContext applicationContext;
    public SparkTaskProcessorDQBody(SparkSession sparkSession, ConfigRestClientApi configRestClientApi, ConfigurableApplicationContext applicationContext) {
        this.sparkSession = sparkSession;
        this.configRestClientApi = configRestClientApi;
        this.applicationContext = applicationContext;
    }


    private void execute(QualityMetricsConfDTO  qualityMetricsConfDTO){

    }

    private TestSetRunner getTestSetRunner(Types.DQMetricsType type) throws Exception {
        return switch (type) {
            case constraint  ->  applicationContext.getBean(ConstraintTestSetRunner.class);

           /* case Types.DQMetricsType.pushDownSQL -> String.format("long %d", );
            case Types.DQMetricsType.sparkSQL    -> String.format("long %d", );
            case Types.DQMetricsType.objectClass -> String.format("long %d", );
            case Double d  -> String.format("double %f", d);
            case String s  -> String.format("String %s", s);
           */ default        -> throw new Exception("Unknown DQMetricsType");
        };
    }

    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        for (QualityMetricsConfDTO  qualityMetricsConfDTO:
                configRestClientApi.getQualityMetricsConfList(scheduledTaskDTO.getDataSetKeyName())){
            for(Map.Entry<String, QualityMetricsConfTestSetDTO> testSet: qualityMetricsConfDTO.getTestSets().entrySet()){
                try {
                    TestSetRunner testSetRunner = getTestSetRunner(testSet.getValue().getDqMetricsType());
                    testSetRunner.run(null,null,null,null,null);
                } catch (Exception e) {
                    throw new TaskFailedException(e);
                }
            }
        }
    }
}
