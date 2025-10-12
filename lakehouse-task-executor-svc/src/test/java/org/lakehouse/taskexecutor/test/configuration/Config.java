package org.lakehouse.taskexecutor.test.configuration;

import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.test.stub.SchedulerRestClientApiErrorTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Primary
public class Config {
    @Bean
    SchedulerRestClientApi getSchedulerRestClientApi() {
        return new SchedulerRestClientApiErrorTest();
    }

}
