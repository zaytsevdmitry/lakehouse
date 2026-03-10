package org.lakehouse.taskexecutor.spark.dq.test;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.BodyParamImpl;
import org.lakehouse.taskexecutor.api.processor.body.CreateTableSparkProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.body.CatalogActivator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.execute.SparkExecuteUtilsImpl;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameterImpl;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.spark.IcebergSparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.spark.dq.configuration.DqMetricConfigProducerKafkaConfigurationProperties;
import org.lakehouse.taskexecutor.spark.dq.configuration.MetricKafkaConfiguration;
import org.lakehouse.taskexecutor.spark.dq.service.ConstraintTestSetRunner;
import org.lakehouse.taskexecutor.spark.dq.service.SparkSQLTestSetRunner;
import org.lakehouse.taskexecutor.spark.dq.service.SparkTaskProcessorDQBody;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQTestSetProducerService;
import org.lakehouse.taskexecutor.spark.dq.service.producer.MetricDQValueProducerService;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
* */
@SpringBootTest
@Import({SparkSQLTestSetRunner.class, ConstraintTestSetRunner.class})
/*@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.api",
                "org.lakehouse.taskexecutor.spark.dq"
        },
        basePackageClasses = {
                CatalogActivatorConfiguration.class,DqMetricConfigProducerKafkaConfigurationProperties.class
        }
)*/
public class SparkTaskProcessorBodyTest {
    static String clientDatasetName = "client_processing";
    static String trnDatasetName = "transaction_processing";
    static String trnddsDatasetName = "transaction_dds";

    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi2() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        JinJavaUtils getJinJavaUtils(){
            return new JinJavaConfiguration().getJinJavaUtils();
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

        @Bean
        @Primary
        DqMetricConfigProducerKafkaConfigurationProperties getDqMetricConfigProducerKafkaConfigurationPropertiesTest(){
            DqMetricConfigProducerKafkaConfigurationProperties p = new DqMetricConfigProducerKafkaConfigurationProperties();
            Map<String,String> props = Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            p.setProperties(props);
            return p;
        }
        @Bean
        @Primary
        MetricKafkaConfiguration getMetricKafkaConfigurationTest(DqMetricConfigProducerKafkaConfigurationProperties p){
            return new MetricKafkaConfiguration(p);
        }

        @Bean
        @Primary
        MetricDQProducerService metricDQProducerService(MetricKafkaConfiguration configuration){
            return  new MetricDQProducerService(configuration.metricDQDTOKafkaTemplate(),"metric_status_test");
        }
        @Bean
        @Primary
        MetricDQTestSetProducerService metricDQTestSetProducerService(MetricKafkaConfiguration configuration){
            return new MetricDQTestSetProducerService(configuration.metricDQTestSetDTOKafkaTemplate(),"metric_test_set_status_test");
        }
        @Bean
        @Primary
        MetricDQValueProducerService metricDQValueProducerService(MetricKafkaConfiguration configuration){
            return  new MetricDQValueProducerService(configuration.metricDQValueDTOKafkaTemplate(),"metric_value");
        }



        @Bean
        @Primary
        SparkTaskProcessorDQBody getSparkTaskProcessorDQBody(
                ConfigRestClientApi configRestClientApi,
                ConfigurableApplicationContext applicationContext,
                MetricDQProducerService metricDQProducerService,
                MetricDQTestSetProducerService metricDQTestSetProducerService,
                MetricDQValueProducerService metricDQValueProducerService,
                SparkSession sparkSession){
            return new SparkTaskProcessorDQBody(
                    configRestClientApi,
                    applicationContext,
                    metricDQProducerService,
                    metricDQTestSetProducerService,
                    metricDQValueProducerService,
                    sparkSession);
        }
        @Bean
        @Primary
        CatalogActivator catalogActivator(SparkSession sparkSession){
            return new CatalogActivator(sparkSession);
        }

    }
    @Autowired
    JinJavaUtils jinJavaUtils;
    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

/*    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
    //    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    //    registry.add("spring.datasource.username", postgres::getUsername);
    //    registry.add("spring.datasource.password", postgres::getPassword);
        registry.add();
    }*/

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

    @Test
    void end2end() throws IOException, TaskFailedException, TaskConfigurationException, URISyntaxException {
        ScheduledTaskDTO scheduledTaskDTO = ObjectMapping
                .fileToObject(
                        new File(getClass().getClassLoader().getResource("en2end.json").toURI()),
                        ScheduledTaskDTO.class);


        SparkSession sparkSession = applicationContext.getBean(SparkSession.class);
        CatalogActivator catalogActivator = applicationContext.getBean(CatalogActivator.class);
        DataSetDTO dataSetDTO = configRestClientApi.getDataSetDTO(scheduledTaskDTO.getDataSetKeyName());
        DataSourceDTO dataSourceDTO = configRestClientApi.getDataSourceDTO(dataSetDTO.getDataSourceKeyName());
        DriverDTO driverDTO = configRestClientApi.getDriverDTO(dataSourceDTO.getDriverKeyName());

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(dataSetDTO.getKeyName());
        jinJavaUtils.cleanGlobalContext();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        SparkExecuteUtils sparkExecuteUtils = new SparkExecuteUtilsImpl(jinJavaUtils,dataSourceDTO,driverDTO, sparkSession);


        SparkSQLDataSourceManipulatorParameter p = new SparkSQLDataSourceManipulatorParameterImpl(
                sparkSession,
                sparkExecuteUtils,
                SQLTemplateFactory.mergeSqlTemplate(driverDTO,dataSourceDTO,dataSetDTO),
                dataSetDTO
        );

        catalogActivator.activate(List.of(dataSourceDTO));
        IcebergSparkSQLDataSourceManipulator m = new IcebergSparkSQLDataSourceManipulator(p);

        CreateTableSparkProcessorBody createTableSparkProcessorBody = new CreateTableSparkProcessorBody(sparkSession);
        BodyParam bodyParam = new BodyParamImpl(m, new HashMap<>(),scheduledTaskDTO.getTaskProcessorArgs());
        createTableSparkProcessorBody.run(bodyParam);
        String sql = jinJavaUtils.render("insert into {{refCat('" + dataSetDTO.getKeyName() + "')}} (" +
                " id ,amount ,client_id, client_name,commission ,provider_id, reg_date_time  )" +
                "values(" +
                "1,   9282.88,1,        'myTestClient', 400.55, 1, timestamp '2025-01-01T00:00:00Z')");
        sparkSession.sql(sql).show();
        sql = jinJavaUtils.render("select * from {{refCat('" + dataSetDTO.getKeyName() +"')}}");
        sparkSession.sql(sql).show();

        SparkTaskProcessorDQBody body = applicationContext.getBean(SparkTaskProcessorDQBody.class);



        body.run(scheduledTaskDTO);



    }
}
