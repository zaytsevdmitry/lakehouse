package org.lakehouse.taskexecutor.test.stub;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.taskexecutor.configuration.RestClientStateConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id=getTestScheduleConfGroup",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.properties.auto.offset.reset=earliest",
                "lakehouse.task-executor.scheduled.task.kafka.consumer.topics=test_send_scheduled_task_topic",
                "lakehouse.client.rest.state.server.url=http://state.test.lakehouse.org:12345",
        })
@EnableConfigurationProperties(value = RestClientStateConfigurationProperties.class)
/*
@ComponentScan(basePackages = {
        "org.lakehouse.taskexecutor"
}
)
*/

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadConfigurationTest {
    @Autowired
    RestClientStateConfigurationProperties restClientStateConfigurationProperties;

    @Test
    @Order(2)
    void testRestStateConfigurationProperties() {
        assert (restClientStateConfigurationProperties.getServer().getUrl().equals("http://state.test.lakehouse.org:12345"));
    }
}
