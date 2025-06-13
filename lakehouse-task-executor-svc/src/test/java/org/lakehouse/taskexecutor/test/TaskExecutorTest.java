package org.lakehouse.taskexecutor.test;

import com.hubspot.jinjava.Jinjava;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.configuration.ImportBeans;
import org.lakehouse.taskexecutor.entity.TaskProcessor;
import org.lakehouse.taskexecutor.service.ProcessorFactory;
import org.lakehouse.taskexecutor.service.TableDefinitionFactory;
import org.lakehouse.taskexecutor.service.TaskProcessorConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
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
                "lakehouse.client.rest.state=http://state.test.lakehouse.org:12345",
        })

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

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


    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
    }
    @Autowired
    ConfigRestClientApi configRestClientApi;

    Jinjava jinjava  = new ImportBeans().Jiinjava();

    private TaskProcessor buildTaskProcessor(
            ScheduledTaskDTO scheduledTaskDTO)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        ScheduledTaskLockDTO t = new ScheduledTaskLockDTO();
        t.setLockId(1L);
        t.setScheduledTaskEffectiveDTO(scheduledTaskDTO);
        TableDefinitionFactory tdf = new TableDefinitionFactory();
        TaskProcessorConfigFactory pcf = new TaskProcessorConfigFactory(configRestClientApi, tdf, jinjava);
        ProcessorFactory pf = new ProcessorFactory(pcf, new StateRestClientApi() {
            @Override
            public int setDataSetStateDTO(DataSetStateDTO dataSetStateDTO) {
                return 0;
            }

            @Override
            public DataSetStateResponseDTO getDataSetStateResponseDTO(DataSetIntervalDTO dataSetIntervalDTO) {
                return null;
            }
        },jinjava);
        return pf.buildProcessor(t);
    }

    private ScheduledTaskDTO getLoadTaskByDatasetName(
            ScheduleEffectiveDTO scheduleEffectiveDTO,
            String dataSetKeyName){
        TaskDTO taskDTO = scheduleEffectiveDTO
                .getScenarioActs()
                .stream()
                .filter(s -> s.getDataSet().equals(dataSetKeyName))
                .flatMap(s -> s.getTasks().stream().filter(t-> t.getName().equals("load")))
                .toList().get(0);
        ScheduledTaskDTO result = new ScheduledTaskDTO();
        result.setTargetDateTime(DateTimeUtils.nowStr());
        result.setIntervalStartDateTime("{{ " + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + " }}");
        result.setIntervalEndDateTime("{{ adddays(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + "}, -1)}");
        result.setName(taskDTO.getName());
        result.setDataSetKeyName(dataSetKeyName);
        result.setExecutionModuleArgs(taskDTO.getExecutionModuleArgs());
        result.setExecutionModule(taskDTO.getExecutionModule());
        result.setScheduleKeyName(scheduleEffectiveDTO.getName());
        result.setScenarioActKeyName("");
        return result;
    }
    @Test
    @Order(1)
    void testExecutionModules()
            throws
            ClassNotFoundException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException,
            NoSuchMethodException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("client_processing");
        DataStoreDTO pgDs = configRestClientApi.getDataStoreDTO(ds.getDataStoreKeyName());
        Map<String,String> testProperties = new HashMap<>();
        testProperties.put("user", pgUser);
        testProperties.put("password", pgPwd);
        pgDs.setProperties(testProperties);
        pgDs.setUrl(pgUrl);
        configRestClientApi.postDataStoreDTO(pgDs);

        //first postgres
        TaskProcessor clientProcessingTaskProcessor =
                buildTaskProcessor(
                    getLoadTaskByDatasetName(scheduleEffectiveDTO,"client_processing"));

        assert (clientProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));

        //second postgres
        TaskProcessor transactionsProcessingTaskProcessor = buildTaskProcessor(
                getLoadTaskByDatasetName(scheduleEffectiveDTO,"transaction_processing"));
        assert (transactionsProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));


        //third spark join two postgres tables outside from db
        TaskProcessor transactionDDSTaskProcessor = buildTaskProcessor(
                getLoadTaskByDatasetName(scheduleEffectiveDTO,"transaction_dds"));
        assert (transactionDDSTaskProcessor.runTask().equals(Status.Task.SUCCESS));
    }

}
