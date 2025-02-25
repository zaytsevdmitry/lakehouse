package org.lakehouse.taskexecutor.test;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaDeserializer;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.configuration.ScheduledTaskKafkaConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.topics=test_send_scheduled_task_topic",
        })
@EnableConfigurationProperties(value = ScheduledTaskKafkaConfigurationProperties.class)
@ComponentScan(basePackages = {
        "org.lakehouse.taskexecutor.configuration",

})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class TaskExecutorTest {
    @Value("${lakehouse.task-executor.scheduled.task.kafka.consumer.topics}") String topic;
    @Bean
    SchedulerRestClientApi getSchedulerRestClientApi(){
        return new SchedulerRestClientApiErrorTest();
    }
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.task-executor.scheduled.task.kafka.consumer.bootstrap.servers", kafka::getBootstrapServers);
    }
    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @BeforeAll
    static void beforeAll() {
        kafka.start();

    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
    }
    private Producer<String, ScheduledTaskMsgDTO> getKafkaProducer() {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduledTaskMsgKafkaDeserializer.class);
        // more standard configuration
        return new DefaultKafkaProducerFactory<String,ScheduledTaskMsgDTO>(props).createProducer();
    }

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
}
