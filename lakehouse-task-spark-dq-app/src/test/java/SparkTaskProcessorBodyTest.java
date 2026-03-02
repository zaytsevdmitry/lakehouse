package org.lakehouse.taskexecutor;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.body.sql.MergeSQLProcessorBody;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Import({MergeSQLProcessorBody.class})
public class SparkTaskProcessorBodyTest {
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        JinJavaUtils getJinJavaUtils(){
            return new JinJavaConfiguration().getJinJavaUtils();
        }
    }
    @Autowired
    JinJavaUtils jinJavaUtils;
    @Autowired
    ConfigurableApplicationContext applicationContext;

    String clientDatasetName = "client_processing";
    String trnDatasetName = "transaction_processing";
    String trnddsDatasetName = "transaction_dds";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );
    ConfigRestClientApi configRestClientApi = new ConfigRestClientApiTest();
    public SparkTaskProcessorBodyTest() throws IOException {
    }

    @BeforeAll
    static void beforeAllStart() {
        kafka.start();
        postgres.start();
    }

    @AfterAll
    static void afterAllDown(){
        kafka.stop();
        postgres.stop();
    }

    public SparkSession buildSparkSession(ScheduledTaskDTO t, SourceConfDTO sourceConfDTO){
        Map<String, Object> conf = new HashMap<>();
        sourceConfDTO.getDataSources().forEach((s, dataSourceDTO) -> {
            conf.putAll(SparkConfUtil.startWithSpark(dataSourceDTO.getService().getProperties()));
        });
        conf.putAll(SparkConfUtil.extractAppConf(t.getTaskProcessorArgs()));
        return SparkSession.builder().master("local").config(conf).getOrCreate();
    }


}
