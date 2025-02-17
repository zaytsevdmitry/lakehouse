package org.lakehouse.scheduler.test.configuration;

import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class ClientApiConfiguration {


    @Bean(name = "configRestClientApi")
    ConfigRestClientApi getClientApi() throws IOException {
        return new ConfigRestClientApiTest();
    }
}
