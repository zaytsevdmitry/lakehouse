package org.lakehouse.scheduler.test;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.service.ScheduleInstanceBuildService;
import org.lakehouse.scheduler.service.ScheduleInstanceLastBuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.lakehouse.test.config.configuration.FileLoader;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
        "lakehouse.client.rest.config.server.url=",
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class SchedulesTest {
    private static final Logger log = LoggerFactory.getLogger(SchedulesTest.class);
    //services
    @Autowired ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;
    @Autowired
    ScheduleInstanceBuildService scheduleInstanceBuildService;


    FileLoader fileLoader = new FileLoader();

    //repository

    @Autowired
    ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");



    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
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

    @Test
    @Order(1)
    public void registration() throws IOException {
        ScheduleEffectiveDTO sef = fileLoader.loadScheduleEffectiveDTO() ;
        Producer<String, ScheduleEffectiveDTO> producer = getKafkaProducer();
        producer.send(new ProducerRecord<String,ScheduleEffectiveDTO>("testtopic",sef.getName(),sef));
        producer.flush();

        scheduleInstanceLastBuildService.findAndRegisterNewSchedule(sef);

        ScheduleInstanceLastBuild sil =  scheduleInstanceLastBuildRepository
                .findByConfigScheduleKeyName(sef.getName())
                .orElseThrow();
        assert (Objects.equals(sil.getLastChangeNumber(), sef.getLastChangeNumber()));

        scheduleInstanceBuildService.buildNewSchedules();
    }

    @Test
    @Order(2)
    public void buildTasks(){

    }


   /* @Test
    public void fullpipeline() throws Exception {

        List<ScheduleEffectiveDTO> scheduleDTOsExpect = new ArrayList<>();
        scheduleDTOsExpect.add(fileLoader.loadScheduleEffectiveDTO());
        OffsetDateTime scheduleChangeDateTimeStr = DateTimeUtils.now();


        server.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/%s", Endpoint.EFFECTIVE_SCHEDULES_FROM_DT, scheduleChangeDateTimeStr)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(scheduleDTOsExpect), MediaType.APPLICATION_JSON));
        System.out.println("scenario is loaded");


        List<ScheduleEffectiveDTO> scheduleEffectiveDTOS =
                clientApi.getScheduleEffectiveDTOList(scheduleChangeDateTimeStr);

        ScenarioActTemplateDTO scenarioActTemplateDTO = fileLoader.loadScenarioActTemplateDTO();

        server.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/%s", Endpoint.SCENARIOS, scenarioActTemplateDTO.getName())))
                .andRespond(withSuccess(objectMapper.writeValueAsString(scenarioActTemplateDTO), MediaType.APPLICATION_JSON));

        System.out.println("scenario is loaded");

        scheduleInstanceLastBuildService.findAndRegisterNewSchedules(scheduleEffectiveDTOS);
        assert (scheduleInstanceLastBuildRepository.findAll().size() ==1);
    }*/

}
