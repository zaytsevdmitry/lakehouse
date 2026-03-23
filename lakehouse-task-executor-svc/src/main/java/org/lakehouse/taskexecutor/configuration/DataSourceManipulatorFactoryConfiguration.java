package org.lakehouse.taskexecutor.configuration;

import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceManipulatorFactoryConfiguration {
    @Bean
    DataSourceManipulatorFactory dataSourceManipulatorFactory(){
        return new DataSourceManipulatorFactoryImpl();
    }
}
