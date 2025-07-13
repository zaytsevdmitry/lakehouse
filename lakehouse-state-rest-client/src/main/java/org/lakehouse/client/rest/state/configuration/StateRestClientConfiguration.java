package org.lakehouse.client.rest.state.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class StateRestClientConfiguration {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Bean
	StateRestClientApi getStateRestClientApi(
			RestClient.Builder builder,
			@Value("${lakehouse.client.rest.state.server.url}") String baseURI){

		logger.info("rest state baseURI:{}", baseURI);
		DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
		defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return
				new StateRestClientApiImpl(
						new RestClientHelper(
								builder
										.uriBuilderFactory(defaultUriBuilderFactory)
										.build()));
	}
}
