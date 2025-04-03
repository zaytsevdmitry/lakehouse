package org.lakehouse.scheduler.test;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.repository.*;
import org.lakehouse.scheduler.service.BuildService;
import org.lakehouse.scheduler.service.ManageStateService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.*;

import org.lakehouse.test.config.configuration.FileLoader;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "lakehouse.scheduler.config.schedule.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.scheduler.config.schedule.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.scheduler.schedule.task.kafka.producer.topic=test_send_scheduled_task_topic",
                "scheduling.enabled: false"
})
@EnableConfigurationProperties(value = ScheduleConfigConsumerKafkaConfigurationProperties.class)
@ComponentScan(basePackages = {
        "org.lakehouse.scheduler"
    }
//        , excludeFilters={@ComponentScan.Filter(type = FilterType.ANNOTATION, value= EnableScheduling.class)}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class SchedulesTest {
    private static final Logger staticLogger = LoggerFactory.getLogger(SchedulesTest.class);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // override bean
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary //may omit this if this is the only SomeBean defined/visible
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest();
        }
    }

    @Autowired ConfigRestClientApi configRestClientApi;

    //services
    @Autowired
    BuildService buildService;

    @Autowired
    ScheduleTaskInstanceService scheduleTaskInstanceService;

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

    @Autowired
    ManageStateService manageStateService;

    @Autowired
    ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
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
        assert (scheduleInstanceLastBuildRepository.findAll().isEmpty());
        // check uncleaned state
        List<ScheduleInstanceLastBuild> sibList = scheduleInstanceLastBuildRepository.findAll();
        assert (sibList.isEmpty());
        // Rise config
        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO() ;
        Producer<String, ScheduleEffectiveDTO> producer = getKafkaProducer();
        producer.send(new ProducerRecord<String,ScheduleEffectiveDTO>("testtopic",sef.getName(),sef));
        producer.flush();

        // registration
        buildService.registration(sef);

        // check duplicates
        sibList = scheduleInstanceLastBuildRepository.findAll();
        assert (sibList.size() == 1);

        
        assert (sibList.get(0).getScheduleInstance() == null);
        assert (Objects.equals(sibList.get(0).getLastChangeNumber(), sef.getLastChangeNumber()));
        assert (sibList.get(0).getConfigScheduleKeyName().equals(sef.getName()));
        assert (sibList.get(0).getLastChangedDateTime() != null);
        assert (sibList.get(0).getLastUpdateDateTime() != null);

    }
    @Test
    @Order(2)
    public void buildTasks() throws IOException {
        registration();
        buildService.buildAll();
        List<ScheduleInstance> siList = scheduleInstanceRepository.findAll();
        assert (siList.size() == 1);
        List<ScheduleTaskInstance> stiList = scheduleTaskInstanceRepository.findByStatus(Status.Task.NEW.label);
        assert (stiList.size() == 24);
    }
    private void run(){
        int rows;

        rows = manageStateService.runAll();
        logger.info("Run schedules {}", rows );

        rows = manageStateService.runNewScenariosActs();
        logger.info("runNewScenariosActs {}", rows );

        rows = scheduleTaskInstanceService.addTaskToQueue();
        logger.info("queueTasks {}", rows );

        rows = scheduleTaskInstanceService.produceScheduledTasks();
        logger.info("produceScheduledTasks {}", rows );


        rows = manageStateService.successSchedules();
        logger.info("Success schedules {}", rows );
    }
    @Test
    @Order(3)
    public void lockUnLock() throws IOException {
        cleanAll();

        // 1)  load schedule config
        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO();
        sef.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(DateTimeUtils.now().minusDays(1L)));
        sef.setIntervalExpression("0 0 0 * * *"); // every day
        // 2) registration schedule
        buildService.registration(sef);

        // 3) Build schedule

        buildService.buildAll(); // create schedule
        buildService.buildAll(); // expect ignore
        buildService.buildAll(); // expect ignore
        assert (scheduleInstanceLastBuildRepository.findAll().size() ==1);

        run(); // expect run
        run(); // expect ignore
        run(); // expect ignore
        scheduleInstanceRepository.findAll().forEach( a ->  System.out.println(a.getConfigScheduleKeyName() + " --> " + a.getTargetExecutionDateTime() + " --> " + a.getStatus()));

        scheduleScenarioActInstanceRepository.findAll().forEach( a ->  System.out.println(a.getName() + " --> " + a.getStatus()));
        scheduleTaskInstanceRepository.findAll().forEach(t -> System.out.println( t.getName() + " --> " + t.getStatus()));
        assert (scheduleInstanceRunningRepository.findAll().size() == 1);
        List<ScheduleInstanceRunning>  list =  scheduleInstanceRunningRepository.findAll();
        List<ScheduleTaskInstance> stilq = scheduleTaskInstanceRepository.findByStatus(Status.Task.QUEUED.label);
        assert (stilq.size() == 2);
        scheduleTaskInstanceService.lockTaskById(stilq.get(0).getId(),"test0");
        scheduleTaskInstanceService.lockTaskById(stilq.get(1).getId(),"test1");
        assert (scheduleTaskInstanceExecutionLockRepository.findAll().size() ==2);
        //todo release task/ fail task / reset lock
    }

}
