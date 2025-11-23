package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SparkProcessorBodyStarter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(SparkProcessorBodyStarter.class);
        if (args.length >= 1) {

            SparkSession sparkSession = SparkSession.builder().getOrCreate();
            SparkProcessorBody body = SparkProcessorBodyFactory.buildSparkProcessorBody(sparkSession, args);
            body.run();

        } else {
            String msg = "No one attribute found. TaskProcessorConfig is null. Exit";
            logger.info(msg);
            throw new Exception(msg);
        }
    }
}
