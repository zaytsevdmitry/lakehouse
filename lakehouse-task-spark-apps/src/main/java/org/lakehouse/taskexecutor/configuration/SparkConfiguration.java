package org.lakehouse.taskexecutor.configuration;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfiguration {
    @Bean
    SparkSession getSparkSession(){
        return SparkSession.builder().getOrCreate();
    }
}
