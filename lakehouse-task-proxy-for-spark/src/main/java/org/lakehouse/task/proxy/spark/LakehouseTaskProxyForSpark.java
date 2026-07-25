package org.lakehouse.task.proxy.spark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LakehouseTaskProxyForSpark {
    public static void main(String[] args) {
        SpringApplication.run(LakehouseTaskProxyForSpark.class, args);
    }
}
