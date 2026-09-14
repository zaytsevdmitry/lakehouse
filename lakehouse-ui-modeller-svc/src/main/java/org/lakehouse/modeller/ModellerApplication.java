package org.lakehouse.modeller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lakehouse Modeller — API gateway, multi-user server-side workspace
 * manager and static frontend server for declarative metadata (YAML) editing.
 */
@SpringBootApplication(scanBasePackages = {
        "org.lakehouse.modeller",
        "org.lakehouse.health"})
@ConfigurationPropertiesScan
@EnableScheduling
public class ModellerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModellerApplication.class, args);
    }
}