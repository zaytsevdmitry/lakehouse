package org.lakehouse.client.rest.config.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestClientConfigurationTest {
	
	
	
    @Bean
    RestClient restClient(
    		RestClient.Builder builder) {

    	
    	 DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory("");
         defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


         return builder
                 .uriBuilderFactory(defaultUriBuilderFactory)
                 .build();
	}
    
    

}
