package org.lakehouse.taskexecutor.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.api.processor.body.sql.AppendSQLProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.sql.CreateTableSQLProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.sql.MergeSQLProcessorBody;
import org.lakehouse.taskexecutor.configuration.DataSourceManipulatorFactoryConfiguration;
import org.lakehouse.taskexecutor.processor.jdbc.JdbcTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.DependencyCheckStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.LockedStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.SuccessStateTaskProcessor;
import org.lakehouse.taskexecutor.test.stub.StateRestClientApiTest;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * VM options for Spark java 17 capability :(
 * --add-exports java.base/sun.nio.ch=ALL-UNNAMED
 * --add-opens=java.base/java.lang=ALL-UNNAMED
 * --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
 * --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
 * --add-opens=java.base/java.io=ALL-UNNAMED
 * --add-opens=java.base/java.net=ALL-UNNAMED
 * --add-opens=java.base/java.nio=ALL-UNNAMED
 * --add-opens=java.base/java.util=ALL-UNNAMED
 * --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
 * --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
 * --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
 * --add-opens=java.base/sun.nio.cs=ALL-UNNAMED
 * --add-opens=java.base/sun.security.action=ALL-UNNAMED
 * --add-opens=java.base/sun.util.calendar=ALL-UNNAMED
 * --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED
 */

@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.topics=test_send_scheduled_task_topic",
                "lakehouse.client.rest.state=http://state.test.lakehouse.org:12345",
                "lakehouse.client.rest.spark.server.url=http://localhost:6066/v1/submissions"
        })
@Import({
        JdbcTaskProcessor.class,
        CreateTableSQLProcessorBody.class,
        MergeSQLProcessorBody.class,
        AppendSQLProcessorBody.class,
        LockedStateTaskProcessor.class,
        DependencyCheckStateTaskProcessor.class,
        SuccessStateTaskProcessor.class,
        DataSourceManipulatorFactoryConfiguration.class

})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskExecutorTest {


    @Value("${lakehouse.task-executor.scheduled.task.kafka.consumer.topics}")
    String topic;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.task-executor.scheduled.task.kafka.consumer.bootstrap.servers", kafka::getBootstrapServers);

    }

    @Value("${spring.datasource.url}")
    String pgUrl;
    @Value("${spring.datasource.username}")
    String pgUser;
    @Value("${spring.datasource.password}")
    String pgPwd;

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @BeforeAll
    static void beforeAll() {
        kafka.start();
        postgres.start();

    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
        postgres.stop();
    }

    private Producer<String, ScheduledTaskMsgDTO> getKafkaProducer() {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduledTaskMsgKafkaDeserializer.class);
        // more standard configuration
        return new DefaultKafkaProducerFactory<String, ScheduledTaskMsgDTO>(props).createProducer();
    }


    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        @Primary
        StateRestClientApi getStateRestClientApi(){
            return new StateRestClientApiTest();
        }

        @Bean
        JinJavaUtils getJinJavaUtils(){
            return new JinJavaConfiguration().getJinJavaUtils();
        }
    }


    @Autowired
    ConfigRestClientApi configRestClientApi;

    @Autowired
    ConfigurableApplicationContext applicationContext;
    @Autowired JdbcTaskProcessor jdbcTaskProcessor;

    private void runTaskProcessor(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskConfigurationException, TaskFailedException, JsonProcessingException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        ((TaskProcessor) applicationContext.getBean(scheduledTaskDTO.getTaskProcessor())).runTask(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);
    }

    private ScheduledTaskDTO getTaskByDatasetName(
            ScheduleEffectiveDTO scheduleEffectiveDTO,
            String dataSetKeyName,
            String taskName) throws JsonProcessingException {
        TaskDTO taskDTO = scheduleEffectiveDTO
                .getScenarioActs()
                .stream()
                .filter(s -> s.getDataSet().equals(dataSetKeyName))
                .flatMap(s -> s.getTasks().stream().filter(t -> t.getName().equals(taskName)))
                .toList().get(0);
        ScheduledTaskDTO result = new ScheduledTaskDTO();
        result.setTargetDateTime(DateTimeUtils.nowStr());
        result.setIntervalStartDateTime("2026-02-05T00:00:00.00+03:00");
        result.setIntervalEndDateTime("2026-02-06T00:00:00.00+03:00");
        result.setName(taskDTO.getName());
        result.setDataSetKeyName(dataSetKeyName);
        result.setTaskProcessorArgs(taskDTO.getTaskProcessorArgs());
        result.setTaskProcessorBody(taskDTO.getTaskProcessorBody());
        result.setTaskProcessor(taskDTO.getTaskProcessor());
        result.setScheduleKeyName(scheduleEffectiveDTO.getKeyName());
        result.setScenarioActKeyName("act");
        return result;
    }

    private ServiceDTO parceUrlToServiceDTO(String url, String user, String password) {
        //"jdbc:postgresql://localhost:5432/mydb?sslmode=disable";
        String[] arr = url.replaceAll("jdbc:postgresql://", "").split("/");
        ServiceDTO result = new ServiceDTO();
        result.setUrn(arr[1]);
        result.setHost(arr[0].split(":")[0]);
        result.setPort(arr[0].split(":")[1]);
        result.setProperties(Map.of("user", user, "password", password));
        return result;
    }

    @Test
    @Order(1)
    void  testExecutionModules()
            throws TaskConfigurationException, TaskFailedException, JsonProcessingException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("client_processing");
        DataSourceDTO pgDs = configRestClientApi.getDataSourceDTO(ds.getDataSourceKeyName());
        pgDs.setService(parceUrlToServiceDTO(pgUrl, pgUser, pgPwd));
        configRestClientApi.postDataStoreDTO(pgDs);

        //first postgres
        //create

        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "prepare"));
        //load
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "load"));

        //second postgres
        runTaskProcessor(
                getTaskByDatasetName(scheduleEffectiveDTO, "transaction_processing", "prepare"));

        runTaskProcessor(
                getTaskByDatasetName(scheduleEffectiveDTO, "transaction_processing", "load"));
    }

    @Test
    @Order(2)
    void shouldBuildStateTaskProcessor() throws TaskConfigurationException, TaskFailedException, JsonProcessingException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("transaction_dds");
        DataSourceDTO pgDs = configRestClientApi.getDataSourceDTO(ds.getDataSourceKeyName());
        pgDs.setService(parceUrlToServiceDTO(pgUrl, pgUser, pgPwd));
        configRestClientApi.postDataStoreDTO(pgDs);

        //begin
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "begin"));
        //assert (begin.runTask().equals(Status.Task.SUCCESS));
        // check =
      /*  runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "check"));*/
        //assert (check.runTask().equals(Status.Task.SUCCESS));
        // finally =
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "finally"));
        //assert (fin.runTask().equals(Status.Task.SUCCESS));
    }

    @Test
    void testPk() throws TaskConfigurationException, TaskFailedException, JsonProcessingException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        //create
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "prepare"));
        //load
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "load"));
    }

}
