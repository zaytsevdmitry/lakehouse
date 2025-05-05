package org.lakehouse.client.rest.config.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.config.ConfigRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class ConfigRestClientConfiguration {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Bean
	ConfigRestClientApi getConfigRestClientApi(
			RestClient.Builder builder,
			@Value("${lakehouse.client.rest.config.server.url}") String baseURI){

		logger.info("rest config baseURI:{}", baseURI);
		DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
		defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return
				new ConfigRestClientApiImpl(
						new RestClientHelper(
								builder
										.uriBuilderFactory(defaultUriBuilderFactory)
										.build()));
	}
}
