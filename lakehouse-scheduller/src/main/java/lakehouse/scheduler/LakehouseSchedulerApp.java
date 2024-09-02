package lakehouse.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LakehouseSchedulerApp {
    public static void main(String[] args) {

        SpringApplication.run(LakehouseSchedulerApp.class, args);
    }
}