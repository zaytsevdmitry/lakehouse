package org.lakehouse.client.rest.taskexecutor.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.taskexecutor.TaskExecutorRestClientApi;
import org.lakehouse.client.rest.taskexecutor.TaskExecutorRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class TaskExecutorRestClientConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    TaskExecutorRestClientApi getTaskExecutorRestClientApi(
            RestClient.Builder builder,
            @Value("${lakehouse.client.rest.taskexecutor.server.url}") String baseURI) {

        logger.info("rest state baseURI:{}", baseURI);
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return
                new TaskExecutorRestClientApiImpl(
                        new RestClientHelper(
                                builder
                                        .uriBuilderFactory(defaultUriBuilderFactory)
                                        .build()));
    }
}
