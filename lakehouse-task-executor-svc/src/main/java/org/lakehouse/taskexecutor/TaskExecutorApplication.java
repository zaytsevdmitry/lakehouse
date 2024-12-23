package org.lakehouse.taskexecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration
public class TaskExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskExecutorApplication.class, args);
    }
}
