package org.lakehouse.scheduler.test.configuration;

import org.junit.jupiter.api.Order;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;


public class ClientApiConfigurationTest {


  /*
    ConfigRestClientApi getConfigRestClientApi() throws IOException {
        return new ConfigRestClientApiTest();
    }*/
}
