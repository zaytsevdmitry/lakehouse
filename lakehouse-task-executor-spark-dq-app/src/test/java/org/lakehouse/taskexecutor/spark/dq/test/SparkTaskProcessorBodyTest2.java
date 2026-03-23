package org.lakehouse.taskexecutor.spark.dq.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.exception.ScriptBuildException;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.lakehouse.taskexecutor.api.datasource.exception.DropException;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.spark.dq.configuration.DqMetricConfigProducerKafkaConfigurationProperties;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootTest(properties = {
       "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.status.topic=metric_status",
        "lakehouse.client.rest.config.server.url=http://192.1.193.80:8080",
        "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic=metric_value",
        "lakehouse.taskexecutor.body.config.dq.kafka.producer.testSet.status.topic=metric_test_set_status"
})
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.spark.dq"
        })
public class SparkTaskProcessorBodyTest2 {
    static String clientDatasetName = "client_processing";
    static String trnDatasetName = "transaction_processing";
    static String trnddsDatasetName = "transaction_dds";

    @Configuration
    @ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.spark.dq"
        })
    @EnableConfigurationProperties(DqMetricConfigProducerKafkaConfigurationProperties.class)
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi3() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        @Primary
        SparkSession getSparkSessionTest() throws IOException {
            ConfigRestClientApi configRestClientApi1 =  new ConfigRestClientApiTest();
            DataSetDTO dataSetDTO = configRestClientApi1.getDataSetDTO(trnddsDatasetName);
            DataSourceDTO dataSourceDTO = configRestClientApi1.getDataSourceDTO(dataSetDTO.getDataSourceKeyName());
            SparkConf conf = new SparkConf();
            SparkConfUtil.extractAppConf(dataSourceDTO.getService().getProperties()).forEach(conf::set);
            return SparkSession.builder().master("local").config(conf).getOrCreate();
        }
    }
    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Autowired ConfigRestClientApi configRestClientApi;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );
    public SparkTaskProcessorBodyTest2() throws IOException {
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
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers",kafka::getBootstrapServers);
    }
    @Test
    void end2end() throws IOException, TaskFailedException, TaskConfigurationException, URISyntaxException, CreateException, DropException {
        ScheduledTaskDTO scheduledTaskDTO = ObjectMapping
                .fileToObject(
                        new File(getClass().getClassLoader().getResource("en2end.json").toURI()),
                        ScheduledTaskDTO.class);
        SparkSession sparkSession = applicationContext.getBean(SparkSession.class);

        DataSourceManipulator dsm = DataManipulators
                .getIcebergDataSourceManipulator(
                        sparkSession,
                        trnddsDatasetName,
                        applicationContext.getBean(ConfigRestClientApi.class)
                        );

        dsm.createTableIfNotExists();
        String sql = "insert into `lakehouse`.`default`.`transaction_dds` (" +
                " id ,amount ,client_id, client_name,commission ,provider_id, reg_date_time  )" +
                "values(" +
                "1,   9282.88,1,        'myTestClient', 400.55, 1, timestamp '2025-01-01T00:00:00Z')";
        sparkSession.sql(sql).show();
        sql = "select * from `lakehouse`.`default`.`transaction_dds`";
        sparkSession.sql(sql).show();


        ProcessorBody body = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
        body.run(scheduledTaskDTO);
        dsm.drop();
    }
    @Test
    void getScript() throws ScriptBuildException, DropException, CreateException, TaskConfigurationException, IOException, URISyntaxException {
        ScheduledTaskDTO scheduledTaskDTO = ObjectMapping
                .fileToObject(
                        new File(getClass().getClassLoader().getResource("en2end.json").toURI()),
                        ScheduledTaskDTO.class);
        SparkSession sparkSession = applicationContext.getBean(SparkSession.class);

        DataSourceManipulator dsm = DataManipulators
                .getIcebergDataSourceManipulator(
                        sparkSession,
                        trnddsDatasetName,
                        applicationContext.getBean(ConfigRestClientApi.class)
                );

        dsm.createTableIfNotExists();
        String sql = "insert into `lakehouse`.`default`.`transaction_dds` (" +
                " id ,amount ,client_id, client_name,commission ,provider_id, reg_date_time  )" +
                "values(" +
                "1,   9282.88,1,        'myTestClient', 400.55, 1, timestamp '2025-01-01T00:00:00Z')";
        sparkSession.sql(sql).show();
        sql = "select * from `lakehouse`.`default`.`transaction_dds`";
        sparkSession.sql(sql).show();
        QualityMetricsConfDTO qualityMetricsConfDTO = configRestClientApi.getQualityMetricsConf("transaction_dds_qm");
        String s = configRestClientApi.getScriptByListOfReference(qualityMetricsConfDTO.getThresholds().get("non_zero_count_th").getScripts());
        System.out.println(s);
        dsm.drop();
    }
}
