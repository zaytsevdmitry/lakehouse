package org.lakehouse.taskexecutor.configuration;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
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
