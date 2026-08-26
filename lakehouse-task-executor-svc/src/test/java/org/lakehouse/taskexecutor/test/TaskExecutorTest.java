/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.test;

import tools.jackson.core.JacksonException;
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
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.client.rest.state.configuration.StateRestClientConfiguration;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.configuration.ScheduledTaskKafkaConfigurationProperties;
import org.lakehouse.taskexecutor.processor.jdbc.JdbcTaskProcessor;
import org.lakehouse.taskexecutor.test.stub.StateRestClientApiTest;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.test.config.util.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
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



@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.topics=test_send_scheduled_task_topic",
                "lakehouse.client.rest.state=http://state.test.lakehouse.org:12345",
                "lakehouse.client.rest.spark.server.url=http://localhost:6066/v1/submissions"
        })

@EnableConfigurationProperties(value = {
        ScheduledTaskKafkaConfigurationProperties.class})
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor",
                "org.lakehouse.taskexecutor.test",
                "org.lakehouse.client.rest.state",
                "org.lakehouse.health"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                SchedulerRestClientConfiguration.class,
                StateRestClientConfiguration.class})
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
            throws TaskConfigurationException, TaskFailedException, JacksonException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        ((TaskProcessor) applicationContext.getBean(scheduledTaskDTO.getTaskProcessor())).runTask(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);
    }

    private ScheduledTaskDTO getTaskByDatasetName(
            ScheduleEffectiveDTO scheduleEffectiveDTO,
            String dataSetKeyName,
            String taskName) throws JacksonException {
        TaskDTO taskDTO = scheduleEffectiveDTO
                .getScenarioActs()
                .stream()
                .filter(s -> s.getDataSetKeyName().equals(dataSetKeyName))
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
        result.setDriverKeyName(taskDTO.getDriverKeyName());
        result.setSqlTemplate(taskDTO.getSqlTemplate());
        return result;
    }

    private ServiceDTO parceUrlToServiceDTO(String url, String user, String password) {
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
            throws TaskConfigurationException, TaskFailedException, JacksonException {
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
    void shouldBuildStateTaskProcessor() throws TaskConfigurationException, TaskFailedException, JacksonException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        DataSetDTO ds = configRestClientApi.getDataSetDTO("transaction_dds");
        DataSourceDTO pgDs = configRestClientApi.getDataSourceDTO(ds.getDataSourceKeyName());
        pgDs.setService(parceUrlToServiceDTO(pgUrl, pgUser, pgPwd));
        configRestClientApi.postDataStoreDTO(pgDs);

        //begin
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "begin"));

        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, ds.getKeyName(), "finally"));

    }

    @Test
    void testPk() throws TaskConfigurationException, TaskFailedException, JacksonException {
        ScheduleEffectiveDTO scheduleEffectiveDTO = configRestClientApi.getScheduleEffectiveDTO(null);
        //create
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "prepare"));
        //load
        runTaskProcessor(
                        getTaskByDatasetName(scheduleEffectiveDTO, "client_processing", "load"));
    }
    @Test
    void jdbcUrlBuilder() throws IOException {

        FileLoader fileLoader = new FileLoader();
        String dataSetKeyName = "transaction_processing";
        DataSetDTO dataSetDTO = fileLoader.loadDataSetDTO(dataSetKeyName);
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO(dataSetDTO.getDataSourceKeyName());

        String result = dataSourceDTO.getDatabaseProtocol().buildConnectionStringTemplate(
                dataSourceDTO.getService().getHost(),
                Integer.parseInt(dataSourceDTO.getService().getPort()),
                dataSourceDTO.getService().getUrn()
        );

        String expected = "jdbc:postgresql://localhost:5432/postgresDB";
        System.out.println(expected);
        System.out.println(result);
        assert (result.equals(expected));

    }
}
