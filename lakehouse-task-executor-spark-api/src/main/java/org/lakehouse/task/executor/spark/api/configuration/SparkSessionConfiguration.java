package org.lakehouse.task.executor.spark.api.configuration;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkSessionConfiguration {
    @Bean() //destroyMethod = "stop"
    SparkSession getSparkSession(){
        return SparkSession.builder().getOrCreate();
    }
}
