package org.lakehouse.config.rest.client.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestClientConfiguration {
	
	
	
    @Bean
    RestClient restClient(
    		RestClient.Builder builder, 
    		@Value("${lakehouse.cinfig.client.server.url}") String baseURI) {
    	System.out.println(baseURI);
    	
    	 DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
         defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


         return builder
                 .uriBuilderFactory(defaultUriBuilderFactory)
                 .build();
	}
    
    

}
