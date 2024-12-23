package org.lakehouse.scheduler;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@TestConfiguration
public class TestConf {
	
	//@Value("${lakehouse.confg.client.server.url")
	private String baseURI = "";//"http://127.0.0.1";
	
    @Bean(name = "restClient")
    RestClient restClient(RestClient.Builder builder) {
    	 DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);   //set baseUrl here
         defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);  //This is the place to configure the url encoding behaviour


         return builder
                 //.baseUrl(baseURI)// Setting baseUrl here will not work  https://github.com/spring-projects/spring-framework/issues/32180
                 .uriBuilderFactory(defaultUriBuilderFactory)
                 .build();
	}

}
