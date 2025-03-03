package org.lakehouse.taskexecutor.test;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Constraint;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.configuration.ScheduledTaskKafkaConfigurationProperties;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.service.ProcessorFactory;
import org.lakehouse.taskexecutor.service.TableDefinitionFactory;
import org.lakehouse.taskexecutor.service.TaskProcessorConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
 * */
@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.topics=test_send_scheduled_task_topic",
        })
//@EnableConfigurationProperties(value = ScheduledTaskKafkaConfigurationProperties.class)
/*@ComponentScan(basePackages = {
        "org.lakehouse.taskexecutor.configuration",

})*/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class TaskExecutorTest {
    @Value("${lakehouse.task-executor.scheduled.task.kafka.consumer.topics}") String topic;
    @Bean
    SchedulerRestClientApi getSchedulerRestClientApi(){
        return new SchedulerRestClientApiErrorTest();
    }

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
    @Value("${spring.datasource.url}") String pgUrl;
    @Value("${spring.datasource.username}") String pgUser;
    @Value("${spring.datasource.password}") String pgPwd;

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
        return new DefaultKafkaProducerFactory<String,ScheduledTaskMsgDTO>(props).createProducer();
    }

/*
    @Autowired
    SchedulerRestClientApi schedulerRestClientApi;
    @Test
    @Order(1)
    void testLock(){
        ScheduledTaskMsgDTO scheduledTaskMsgDTO = new ScheduledTaskMsgDTO();
        scheduledTaskMsgDTO.setId(1L);
        scheduledTaskMsgDTO.setTaskExecutionServiceGroupName("test");
        Producer<String, ScheduledTaskMsgDTO> producer = getKafkaProducer();
        producer.send(new ProducerRecord<>(topic,scheduledTaskMsgDTO));
    }
*/

    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
            //may omit this if this is the only SomeBean defined/visible
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest();
        }
    }

    @Autowired
    ConfigRestClientApi configRestClientApi;

    private TaskProcessor buildTaskProcessor(
            TaskDTO taskDTO,
            String dataSetKeyName)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        ScheduledTaskLockDTO t = new ScheduledTaskLockDTO();
       /* TaskDTO eff = ;
        eff.setExecutionModuleArgs(new HashMap<>());
        eff.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.SparkTaskProcessor");
        eff.setName("transaction_dds");
       */
        t.setDataSetKeyName(dataSetKeyName);
        t.setLockId(1L);
        t.setScheduledTaskEffectiveDTO(taskDTO);
        t.setScheduleTargetDateTime("2025-01-01 00:00:00.0Z");
        TableDefinitionFactory tdf = new TableDefinitionFactory();
        TaskProcessorConfigFactory pcf = new TaskProcessorConfigFactory(configRestClientApi, tdf);
        ProcessorFactory pf = new ProcessorFactory(pcf);
        return pf.buildProcessor(t);
    }

    private TaskDTO getLoadTaskByDatasetName(ScheduleEffectiveDTO scheduleEffectiveDTO,String dataSetKeyName){
        return scheduleEffectiveDTO
                .getScenarioActs()
                .stream()
                .filter(s -> s.getDataSet().equals(dataSetKeyName))
                .flatMap(s -> s.getTasks().stream().filter(t-> t.getName().equals("load")))
                .toList().get(0);

    }
    @Test
    @Order(1)
    void testExecutionModules() throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("client_processing");
        DataStoreDTO pgDs = configRestClientApi.getDataStoreDTO(ds.getDataStore());
        Map<String,String> testProperties = new HashMap<>();
        testProperties.put("user", pgUser);
        testProperties.put("password", pgPwd);
        pgDs.setProperties(testProperties);
        pgDs.setUrl(pgUrl);
        configRestClientApi.postDataStoreDTO(pgDs);

        //first postgres
        TaskProcessor clientProcessingTaskProcessor = buildTaskProcessor(
                getLoadTaskByDatasetName(scheduleEffectiveDTO,"client_processing"), "client_processing");

        assert (clientProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));

        //second postgres
        TaskProcessor transactionsProcessingTaskProcessor = buildTaskProcessor(
                getLoadTaskByDatasetName(scheduleEffectiveDTO,"transaction_processing"), "transaction_processing");
        assert (transactionsProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));


        //third spark join two postgres tables outside from db
        TaskProcessor transactionDDSTaskProcessor = buildTaskProcessor(
                getLoadTaskByDatasetName(scheduleEffectiveDTO,"transaction_dds"), "transaction_dds");
        assert (transactionDDSTaskProcessor.runTask().equals(Status.Task.SUCCESS));
    }

}
