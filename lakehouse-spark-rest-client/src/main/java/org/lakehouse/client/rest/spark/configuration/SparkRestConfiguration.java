package org.lakehouse.client.rest.spark.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.SparkRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class SparkRestConfiguration {
    Logger logger = LoggerFactory.getLogger(SparkRestConfiguration.class);

    @Bean
    SparkRestClientApi sparkRestClientApi(
            RestClient.Builder builder,
            @Value("${lakehouse.client.rest.spark.server.url}") String baseURI
    ) {
        logger.info("lakehouse.client.rest.spark.server.url={}", baseURI);
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return new SparkRestClientApiImpl(
                new
                        RestClientHelper(
                        builder.uriBuilderFactory(defaultUriBuilderFactory).build()));
    }
}
