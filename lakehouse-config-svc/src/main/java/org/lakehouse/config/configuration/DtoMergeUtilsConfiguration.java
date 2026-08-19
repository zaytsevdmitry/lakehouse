package org.lakehouse.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.api.utils.DtoMergeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DtoMergeUtilsConfiguration {
    @Bean
    public DtoMergeUtils getDtoMergeUtils(ObjectMapper objectMapper){
        return new DtoMergeUtils(objectMapper);
    }
}
