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
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.service.InternalSchedulerService;
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
    private static final Logger log = LoggerFactory.getLogger(SchedulesTest.class);

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


        buildService.registration(sef);
        // check duplicates
        List<ScheduleInstanceLastBuild> sibList = scheduleInstanceLastBuildRepository.findAll();
        assert (sibList.size() == 1);

        ScheduleInstanceLastBuild sil =  scheduleInstanceLastBuildRepository
                .findByConfigScheduleKeyName(sef.getName())
                .orElseThrow();
        assert (Objects.equals(sil.getLastChangeNumber(), sef.getLastChangeNumber()));
        scheduleInstanceLastBuildRepository.findAll().forEach(
                instanceLastBuild -> log.info("Schedules -=> {}", instanceLastBuild.getConfigScheduleKeyName()));
        buildService.buildAll();


        // check duplicates
/*        List<ScheduleInstanceLastBuild> sibList = scheduleInstanceLastBuildRepository.findAll();
        assert (sibList.size() == 1);*/
        List<ScheduleInstance> siList = scheduleInstanceRepository.findAll();
        assert (siList.size() == 1);
        List<ScheduleTaskInstance> stiList = scheduleTaskInstanceRepository.findByStatus(Status.Task.QUEUED.label);
        assert (stiList.size() == 1);



    }

    @Test
    @Order(1)
    public void lockUnLock() throws IOException {
        cleanAll();

        // 1)  load schedule config
        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO();
        sef.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(DateTimeUtils.now().minusDays(1L)));
        sef.setIntervalExpression("0 0 0 * * *"); // every day
        // 2) registration schedule
        buildService.registration(sef);

        // 3) Build schedule

        internalSchedulerService.build(); // create schedule
        internalSchedulerService.build(); // expect ignore
        internalSchedulerService.build(); // expect ignore
        assert (scheduleInstanceLastBuildRepository.findAll().size() ==1);

        internalSchedulerService.run(); // expect run
        internalSchedulerService.run(); // expect ignore
        internalSchedulerService.run(); // expect ignore
        scheduleInstanceRepository.findAll().forEach( a ->  System.out.println(a.getConfigScheduleKeyName() + " --> " + a.getTargetExecutionDateTime() + " --> " + a.getStatus()));

        scheduleScenarioActInstanceRepository.findAll().forEach( a ->  System.out.println(a.getName() + " --> " + a.getStatus()));
        scheduleTaskInstanceRepository.findAll().forEach(t -> System.out.println( t.getName() + " --> " + t.getStatus()));
        assert (scheduleInstanceRunningRepository.findAll().size() == 1);
        List<ScheduleInstanceRunning>  list =  scheduleInstanceRunningRepository.findAll();
        List<ScheduleTaskInstance> stilq = scheduleTaskInstanceRepository.findByStatus(Status.Task.QUEUED.label);
        assert (stilq.size() == 1);

    }

   /* private List<String> getMatched(String regex, String input, int group ){
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()){
            result.add(matcher.group(group));

        }
        return result;
    }
    @Test
    void testscript() throws IOException {


        ConfigRestClientApiTest configRestClientApiTest = new ConfigRestClientApiTest();
        String regex = "\\$\\{.*?\\}";
        List<String> keys =getMatched(regex,configRestClientApiTest.getScript("transaction_dds"),0);



        Map<String,String> dataSources = new HashMap<>();

        for (String key:keys.stream()
                .filter(string -> string.contains("${source(")).toList()){
            List<String> sourceKeys = getMatched(
                    "\\$\\{source\\((.*?)\\)\\}",
                    key,1);
            if(sourceKeys.isEmpty()){
                throw new Exception();
            }
            else {

            }
        }



                .collect(Collectors.toMap(
                        key-> key,
                        key-> .get(0) ));

        dataSources.forEach((string, string2) -> System.out.println(string + "-> " + string2));


       // System.out.println(getMatched("\\$\\{source\\((.*?)\\)\\}", "${source(transaction_dds)}",1).get(0));

    }
*/
}
