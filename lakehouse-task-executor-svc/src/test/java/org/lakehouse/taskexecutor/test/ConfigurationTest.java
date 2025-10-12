package org.lakehouse.taskexecutor.test;

import org.junit.jupiter.api.Test;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {SparkConfigurationProperties.class})
@TestPropertySource(locations = "classpath:application.yml")
@EnableConfigurationProperties(value = SparkConfigurationProperties.class)
public class ConfigurationTest {
    @Autowired
    SparkConfigurationProperties sparkConfigurationProperties;

    @Test
    void testSparkConf() {
        assert (!sparkConfigurationProperties.getProperties().isEmpty());
    }

}
