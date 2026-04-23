package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.processor.body.sql.SQLProcessorBodyAbstract;

public abstract class SparkProcessorBodyAbstract extends SQLProcessorBodyAbstract {
    private final SparkSession sparkSession;
    public SparkProcessorBodyAbstract(
            ConfigRestClientApi configRestClientApi,
            SparkSession sparkSession,
            DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory);
        this.sparkSession = sparkSession;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }
}
