package org.lakehouse.taskexecutor.executionmodule;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractSparkDeployTaskProcessor extends AbstractTaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final SparkRestClientApi sparkRestClientApi;
	private final SparkConfigurationProperties sparkConfigurationProperties;
	public AbstractSparkDeployTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
			SparkConfigurationProperties sparkConfigurationProperties,
			SparkRestClientApi sparkRestClientApi) {
        super(taskProcessorConfigDTO);
		this.sparkConfigurationProperties = sparkConfigurationProperties;
		this.sparkRestClientApi = sparkRestClientApi;

    }

	public SparkRestClientApi getSparkRestClientApi() {
		return sparkRestClientApi;
	}
	public SparkConfigurationProperties getSparkConfigurationProperties(){ return sparkConfigurationProperties;}
}
