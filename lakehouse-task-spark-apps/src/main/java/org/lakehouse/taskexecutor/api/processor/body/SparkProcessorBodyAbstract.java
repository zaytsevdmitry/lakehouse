package org.lakehouse.taskexecutor.api.processor.body;

import org.apache.spark.sql.SparkSession;

public abstract class SparkProcessorBodyAbstract implements ProcessorBody {
    private final SparkSession sparkSession;
    public SparkProcessorBodyAbstract(SparkSession sparkSession) {


        this.sparkSession = sparkSession;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }
}
