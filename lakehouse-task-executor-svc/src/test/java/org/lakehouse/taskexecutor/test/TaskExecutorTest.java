package org.lakehouse.taskexecutor.test;

import com.hubspot.jinjava.Jinjava;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.factory.TaskConfigBuildException;
import org.lakehouse.taskexecutor.configuration.ImportBeans;
import org.lakehouse.taskexecutor.exception.TaskProcessorConfigurationException;
import org.lakehouse.taskexecutor.api.factory.TaskProcessorConfigFactory;
import org.lakehouse.taskexecutor.factory.TaskProcessorFactory;
import org.lakehouse.taskexecutor.test.stub.ConfigRestClientApiTest;
import org.lakehouse.taskexecutor.test.stub.SparkRestClientApiTest;
import org.lakehouse.taskexecutor.test.stub.StateRestClientApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ContextConfiguration(classes = {
        ImportBeans.class,
        ConfigRestClientApiTest.class,
        JinJavaConfiguration.class,
        SparkRestClientApiTest.class
})
public class TaskExecutorTest {
    @Autowired
    @Qualifier("jinjava")
    Jinjava jinjava;
    @Autowired
    SparkRestClientApi sparkRestClientApi;


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
    }

    @Autowired
    ConfigRestClientApi configRestClientApi;


    private TaskProcessor buildTaskProcessor(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskProcessorConfigurationException, TaskConfigBuildException {
        ScheduledTaskLockDTO t = new ScheduledTaskLockDTO();
        t.setLockId(1L);
        t.setScheduledTaskEffectiveDTO(scheduledTaskDTO);
        TaskProcessorConfigFactory pcf = new TaskProcessorConfigFactory(configRestClientApi, jinjava);
        TaskProcessorFactory pf = new TaskProcessorFactory(new StateRestClientApiTest());
        return pf.buildProcessor(pcf.buildTaskProcessorConfig(t), t.getScheduledTaskEffectiveDTO().getExecutionModule());
    }

    private ScheduledTaskDTO getTaskByDatasetName(
            ScheduleEffectiveDTO scheduleEffectiveDTO,
            String dataSetKeyName,
            String taskName) {
        TaskDTO taskDTO = scheduleEffectiveDTO
                .getScenarioActs()
                .stream()
                .filter(s -> s.getDataSet().equals(dataSetKeyName))
                .flatMap(s -> s.getTasks().stream().filter(t -> t.getName().equals(taskName)))
                .toList().get(0);
        ScheduledTaskDTO result = new ScheduledTaskDTO();
        result.setTargetDateTime(DateTimeUtils.nowStr());
        result.setIntervalStartDateTime("{{ " + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + " }}");
        result.setIntervalEndDateTime("{{ adddays(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ", -1)}}");
        result.setName(taskDTO.getName());
        result.setDataSetKeyName(dataSetKeyName);
        result.setExecutionModuleArgs(taskDTO.getExecutionModuleArgs());
        result.setExecutionModule(taskDTO.getExecutionModule());
        result.setScheduleKeyName(scheduleEffectiveDTO.getName());
        result.setScenarioActKeyName("");
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
            throws TaskProcessorConfigurationException, TaskFailedException, TaskConfigBuildException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("client_processing");
        DataSourceDTO pgDs = configRestClientApi.getDataSourceDTO(ds.getDataSourceKeyName());
        pgDs.setServices(List.of(parceUrlToServiceDTO(pgUrl, pgUser, pgPwd)));
        configRestClientApi.postDataStoreDTO(pgDs);

        //first postgres
        TaskProcessor clientProcessingTaskProcessor =
                buildTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "load"));

        clientProcessingTaskProcessor.runTask();
        //assert (clientProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));

        //second postgres
        TaskProcessor transactionsProcessingTaskProcessor = buildTaskProcessor(
                getTaskByDatasetName(scheduleEffectiveDTO, "transaction_processing", "load"));
        transactionsProcessingTaskProcessor.runTask();
        //assert (transactionsProcessingTaskProcessor.runTask().equals(Status.Task.SUCCESS));


        //third spark join two postgres tables outside from db
       /* TaskProcessor transactionDDSTaskProcessor = buildTaskProcessor(
                getTaskByDatasetName(scheduleEffectiveDTO,"transaction_dds","load"));
        transactionDDSTaskProcessor.runTask();*/
        //assert (transactionDDSTaskProcessor.runTask().equals(Status.Task.SUCCESS));
    }

    @Test
    @Order(2)
    void shouldBuildStateTaskProcessor() throws TaskProcessorConfigurationException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, TaskFailedException, TaskConfigBuildException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("transaction_dds");
        DataSourceDTO pgDs = configRestClientApi.getDataSourceDTO(ds.getDataSourceKeyName());
        pgDs.setServices(List.of(parceUrlToServiceDTO(pgUrl, pgUser, pgPwd)));
        configRestClientApi.postDataStoreDTO(pgDs);

        TaskProcessor begin =
                buildTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "begin"));
        begin.runTask();
        //assert (begin.runTask().equals(Status.Task.SUCCESS));
        TaskProcessor check =
                buildTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "check"));
        check.runTask();
        //assert (check.runTask().equals(Status.Task.SUCCESS));
        TaskProcessor fin =
                buildTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "finally"));
        fin.runTask();
        //assert (fin.runTask().equals(Status.Task.SUCCESS));
    }


}
