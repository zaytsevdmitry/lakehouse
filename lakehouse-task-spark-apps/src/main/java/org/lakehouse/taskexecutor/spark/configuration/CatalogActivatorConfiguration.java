package org.lakehouse.taskexecutor.spark.configuration;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.api.processor.body.body.CatalogActivator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogActivatorConfiguration {
    private final SparkSession sparkSession;

    public CatalogActivatorConfiguration(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @Bean
    CatalogActivator getCatalogActivator(){
        return new CatalogActivator(sparkSession);
    }
}
