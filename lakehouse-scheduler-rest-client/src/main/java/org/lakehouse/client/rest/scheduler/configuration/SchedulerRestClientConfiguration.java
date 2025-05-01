package org.lakehouse.client.rest.scheduler.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class SchedulerRestClientConfiguration {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
	SchedulerRestClientApi getSchedulerRestClientApi(
			RestClient.Builder builder,
			@Value("${lakehouse.client.rest.scheduler.server.url}") String baseURI) {

		logger.info("rest scheduler baseURI:{}", baseURI);

		DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);

		defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

		return new SchedulerRestClientApiImpl(
				new RestClientHelper(
						builder
								.uriBuilderFactory(defaultUriBuilderFactory)
								.build()));
	}
}
