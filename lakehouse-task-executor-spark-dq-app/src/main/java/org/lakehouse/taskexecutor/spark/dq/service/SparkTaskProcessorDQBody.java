package org.lakehouse.taskexecutor.spark.dq.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQTestSetProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQValueProducerService;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.task.ScheduledTaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SparkTaskProcessorDQBody  implements ProcessorBody {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConfigRestClientApi configRestClientApi;
    private final ConfigurableApplicationContext applicationContext;
    private final MetricDQProducerService metricDQProducerService;
    private final MetricDQTestSetProducerService metricDQTestSetProducerService;
    private final MetricDQValueProducerService metricDQValueProducerService;
    private final SparkSession sparkSession;
    public SparkTaskProcessorDQBody(
            ConfigRestClientApi configRestClientApi,
            ConfigurableApplicationContext applicationContext, MetricDQProducerService metricDQProducerService,
            MetricDQTestSetProducerService metricDQTestSetProducerService, MetricDQValueProducerService metricDQValueProducerService, SparkSession sparkSession) {
        this.configRestClientApi = configRestClientApi;
        this.applicationContext = applicationContext;
        this.metricDQProducerService = metricDQProducerService;
        this.metricDQTestSetProducerService = metricDQTestSetProducerService;
        this.metricDQValueProducerService = metricDQValueProducerService;
        this.sparkSession = sparkSession;
    }

    private List<QualityMetricsConfDTO> getQualityMetricsConfs(ScheduledTaskDTO scheduledTaskDTO){
        List<QualityMetricsConfDTO> qualityMetricsConfs = configRestClientApi
                .getQualityMetricsConfListByDataSetKeyName(scheduledTaskDTO.getDataSetKeyName())
                .stream()
                .filter(QualityMetricsConfDTO::isEnabled)
                .toList();
        if(qualityMetricsConfs.isEmpty()){
            logger.warn("Found no one QualityMetricsConf of {}", scheduledTaskDTO.getDataSetKeyName());
        }else {
            logger.info("Found {} of QualityMetricsConf for {}", qualityMetricsConfs.size(), scheduledTaskDTO.getDataSetKeyName());
        }
        return qualityMetricsConfs;
    }
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException {

        ValidationResult validationResult = ScheduledTaskDTOValidator.validate(scheduledTaskDTO);

        if (!validationResult.isValid()){

            throw new TaskConfigurationException(String.join(";", validationResult.getDescriptions()));
        }
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = prepareJinJavaUtils(sourceConfDTO,scheduledTaskDTO);
        Map<String, Dataset<Row>> thresholds = new HashMap<>();


        for (QualityMetricsConfDTO  qualityMetricsConfDTO: getQualityMetricsConfs(scheduledTaskDTO)) {
            long metricId = getMetricId(sourceConfDTO,scheduledTaskDTO,qualityMetricsConfDTO);
            logger.info("Run QualityMetricsConf {}", qualityMetricsConfDTO.getKeyName());
            logger.info("Run QualityMetricsConf {}", qualityMetricsConfDTO.toString());
            new QualityMetricRunner(
                    metricId,
                    sourceConfDTO,
                    scheduledTaskDTO,
                    qualityMetricsConfDTO,
                    jinJavaUtils,
                    applicationContext,
                    metricDQProducerService,
                    metricDQTestSetProducerService,
                    metricDQValueProducerService,
                    sparkSession
            ).run();
        }
    }
    protected JinJavaUtils prepareJinJavaUtils(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO) throws TaskConfigurationException {
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        Map<String,Object> context = new HashMap<>();
        try {
            context.putAll(ObjectMapping.asMap(sourceConfDTO));
            context.putAll(ObjectMapping.asMap(scheduledTaskDTO));

        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        jinJavaUtils.injectGlobalContext(context);
        return jinJavaUtils;
    }

    private Long getMetricId(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO  scheduledTaskDTO,
            QualityMetricsConfDTO qualityMetricsConfDTO){
        Long id = MurmurHash3.hash128((
                sourceConfDTO.getTargetDataSource().getKeyName() +
                        sourceConfDTO.getTargetDataSet().getDatabaseSchemaName() +
                        sourceConfDTO.getTargetDataSet().getTableName() +
                        qualityMetricsConfDTO.getKeyName() +
                        scheduledTaskDTO.getTargetDateTime() +
                        scheduledTaskDTO.getIntervalStartDateTime() +
                        scheduledTaskDTO.getIntervalEndDateTime()
                ).getBytes())[0];
        return id;
    }


}
