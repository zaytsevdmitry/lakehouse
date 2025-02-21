package org.lakehouse.scheduler.test;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionLockRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.lakehouse.scheduler.service.InternalSchedulerService;
import org.lakehouse.scheduler.service.ScheduleInstanceBuildService;
import org.lakehouse.scheduler.service.ScheduleInstanceLastBuildService;
import org.lakehouse.scheduler.test.configuration.ClientApiConfigurationTest;
import org.lakehouse.scheduler.test.configuration.ConfigRestClientApiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.*;

import org.lakehouse.test.config.configuration.FileLoader;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
        "lakehouse.scheduler.config.schedule.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
        "lakehouse.scheduler.config.schedule.kafka.consumer.properties.auto.offset.reset=earliest",
        "lakehouse.scheduler.schedule.task.kafka.producer.topic=test_send_scheduled_task_topic",
})
@EnableConfigurationProperties(value = ScheduleConfigConsumerKafkaConfigurationProperties.class)
@ComponentScan(basePackages = { "org.lakehouse.scheduler" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class SchedulesTest {
    // override bean
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary //may omit this if this is the only SomeBean defined/visible
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest();
        }
    }
    private static final Logger log = LoggerFactory.getLogger(SchedulesTest.class);
    //services
    @Autowired ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;
    @Autowired
    ScheduleInstanceBuildService scheduleInstanceBuildService;

    @Autowired
    InternalSchedulerService internalSchedulerService;

    FileLoader fileLoader = new FileLoader();

    //repository

    @Autowired
    ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;

    @Autowired
    ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
    @Autowired
    ScheduleTaskInstanceExecutionLockRepository scheduleTaskInstanceExecutionLockRepository;
    @Autowired
    ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    @Autowired
    ScheduleInstanceRepository scheduleInstanceRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.scheduler.config.schedule.kafka.consumer.properties.bootstrap.servers", kafka::getBootstrapServers);
        registry.add("lakehouse.scheduler.schedule.task.kafka.producer.properties.bootstrap.servers", kafka::getBootstrapServers);
    }
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
    private Producer<String, ScheduleEffectiveDTO> getKafkaProducer() {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaSerializer.class);
            // more standard configuration
        return new DefaultKafkaProducerFactory<String,ScheduleEffectiveDTO>(props).createProducer();
    }

    void cleanAll(){
        scheduleInstanceRepository.deleteAll();
    }

    @Test
    @Order(1)
    public void registration() throws IOException {
        cleanAll();
        scheduleInstanceLastBuildRepository.findAll().forEach(instanceLastBuild ->
                log.info("Schedules -=> {}", instanceLastBuild.getConfigScheduleKeyName()));

        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO() ;
        Producer<String, ScheduleEffectiveDTO> producer = getKafkaProducer();
        producer.send(new ProducerRecord<String,ScheduleEffectiveDTO>("testtopic",sef.getName(),sef));
        producer.flush();

        scheduleInstanceLastBuildRepository.findAll()
                .forEach(instanceLastBuild ->
                        log.info("Schedules -=> {}",
                                instanceLastBuild.getConfigScheduleKeyName()));


        scheduleInstanceLastBuildService.findAndRegisterNewSchedule(sef);

        ScheduleInstanceLastBuild sil =  scheduleInstanceLastBuildRepository
                .findByConfigScheduleKeyName(sef.getName())
                .orElseThrow();
        assert (Objects.equals(sil.getLastChangeNumber(), sef.getLastChangeNumber()));
        scheduleInstanceLastBuildRepository.findAll().forEach(instanceLastBuild -> log.info("Schedules -=> {}", instanceLastBuild.getConfigScheduleKeyName()));
        scheduleInstanceBuildService.buildNewSchedules();
        internalSchedulerService.findAndRegisterNewSchedules();
        internalSchedulerService.runSchedules();



    }


}
